package org.hibernate.tool.internal.export.java;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.*;
import org.hibernate.tool.internal.util.NameConverter;

import java.util.Iterator;
import java.util.List;

/**
 * Abstract implementation of POJOClass. To be extended by ComponentPOJO and EntityPOJO
 * @author max
 * @author <a href="mailto:abhayani@jboss.org">Amit Bhayani</a>
 *
 */
abstract public class BasicPOJOClass implements POJOClass, MetaAttributeConstants {

	protected ImportContext importContext;
	protected MetaAttributable meta;
	protected final Cfg2JavaTool c2j;
	
	public BasicPOJOClass(MetaAttributable ma, Cfg2JavaTool c2j) {
		this.meta = Assert.notNull(ma, "class Argument must be not null");
		this.c2j = Assert.notNull(c2j, "c2j must be not null");
	}
	
	// called by subclasses
	protected void init() {
		importContext = new ImportContextImpl(getPackageName());
		
		MetaAttribute metaAttribute = meta.getMetaAttribute("extra-import");
		new ImportHelper(importContext).initializeImports(metaAttribute);

	}
	
	protected String getPackageDeclaration(String pkgName) {
		return PackageUtils.packageDeclaration(pkgName);
	}

	public String getPackageDeclaration() {
		String pkgName = getPackageName();
		return getPackageDeclaration(pkgName);			
	}

	/** Return package name. Note: Does not handle inner classes */ 
	public String getPackageName() {
		String generatedClass = getGeneratedClassName();
		return StringHelper.qualifier(generatedClass.trim());
	}
	
	public String getShortName() {
		return qualifyInnerClass(StringHelper.unqualify(getMappedClassName()));
	}
	
	public String getQualifiedDeclarationName() {
		String generatedName = qualifyInnerClass(getGeneratedClassName());
		String qualifier = StringHelper.qualifier(getMappedClassName());
		if ("".equals( qualifier )) {
			return qualifier + "." + generatedName;
		}

		return generatedName;
	}
	
	/**
	 * @return unqualified classname for this class (can be changed by meta attribute "generated-class")
	 */
	public String getDeclarationName() {
		return qualifyInnerClass(StringHelper.unqualify( getGeneratedClassName() ));
	}
	
	protected String getGeneratedClassName() {
		String generatedClass = getMetaAsString(MetaAttributeConstants.GENERATED_CLASS).trim();
		if(StringHelper.isEmpty(generatedClass) ) {
			generatedClass = getMappedClassName();
		}

		if(generatedClass == null) {
			return ""; // will occur for <dynamic-component>
		}

		return generatedClass;
	}
	
	protected String qualifyInnerClass(String className) {
		return className.replace('$', '.');
	}
	
	protected abstract String getMappedClassName();

	public String getMetaAsString(String attribute) {
		MetaAttribute metaAttribute = meta.getMetaAttribute( attribute );
		return MetaAttributeHelper.getMetaAsString(metaAttribute);
	}

	public boolean hasMetaAttribute(String attribute) {
		return meta.getMetaAttribute( attribute ) != null;
	}

	public String getMetaAsString(String attribute, String separator) {
		return MetaAttributeHelper.getMetaAsString( meta.getMetaAttribute( attribute ), separator );
	}

	public boolean getMetaAsBool(String attribute) {
		return getMetaAsBool( attribute, false );
	}

	public boolean getMetaAsBool(String attribute, boolean defaultValue) {
		return MetaAttributeHelper.getMetaAsBool( meta.getMetaAttribute( attribute ), defaultValue );
	}

	public String getClassJavaDoc(String fallback, int indent) {
		MetaAttribute metaAttribute = meta.getMetaAttribute( CLASS_DESCRIPTION );
		if (metaAttribute == null) {
			return c2j.toJavaDoc(fallback, indent);
		}

		return c2j.toJavaDoc(getMetaAsString(CLASS_DESCRIPTION), indent);
	}
	
	public String getClassModifiers() {
		String classModifiers = null;

		// Get scope (backwards compatibility)
		if ( meta.getMetaAttribute( SCOPE_CLASS ) != null ) {
			classModifiers = getMetaAsString( SCOPE_CLASS ).trim();
		}

		// Get modifiers
		if ( meta.getMetaAttribute( CLASS_MODIFIER ) != null ) {
			classModifiers = getMetaAsString( CLASS_MODIFIER ).trim();
		}
		return classModifiers == null ? "public" : classModifiers;
	}

	public String getDeclarationType() {
		boolean isInterface = isInterface();
		if ( isInterface ) {
			return INTERFACE;
		}

		return "class";
	}
	
	public boolean isInterface() {
		return getMetaAsBool( INTERFACE );
	}
	
	public String getExtendsDeclaration() {
		String extendz = getExtends();
		if ( extendz == null || extendz.trim().length() == 0 ) {
			return "";
		}

		return "extends " + extendz;
	}

	public String getImplementsDeclaration() {
		String implementz = getImplements();
		if ( implementz == null || implementz.trim().length() == 0 ) {
			return "";
		}
		
		return "implements " + implementz;
	}
	
	public String generateEquals(String thisName, String otherName, boolean useGenerics) {
		Iterator<Property> allPropertiesIterator = getEqualsHashCodePropertiesIterator();
		return generateEquals(thisName, otherName, allPropertiesIterator, useGenerics);
	}
	
	/** returns the properties that would be visible on this entity as a pojo. This does not return *all* properties since hibernate has certain properties that are only relevant in context of persistence. */ 
	public abstract Iterator<Property> getAllPropertiesIterator();

	protected String generateEquals(String thisName, String otherName, Iterator<Property> allPropertiesIterator, boolean useGenerics) {
		return new EqualsAndHashCodeHelper(c2j, this, useGenerics).generateEquals(thisName, otherName, allPropertiesIterator);
	}

	public String getExtraClassCode() {
		return getMetaAsString( "class-code", "\n" );
	}

	public boolean needsEqualsHashCode() {
		Iterator<Property> iter = getAllPropertiesIterator();
		return new EqualsAndHashCodeHelper(c2j, this, false).needsEqualsAndHashCode(iter);
	}

	public abstract String getExtends();
	
	public abstract String getImplements();

	
	public String importType(String fqcn) {
		return importContext.importType(fqcn);
	}
	
	public String generateImports() {
		return importContext.generateImports();
	}

	public String staticImport(String fqcn, String member) {
		return importContext.staticImport(fqcn, member);
	}
	
	public String generateBasicAnnotation(Property property) {
		return new AnnotationsHelper(this).basicAnnotationsTo(property);
	}

	public String generateAnnColumnAnnotation(Property property) {
		return new AnnotationsHelper(this).generateColumnAnnotations(property);
	}

	protected void appendCommonColumnInfo(StringBuffer annotations, Column column, boolean insertable, boolean updatable) {
		new AnnotationsHelper(this).addAnnotationsProperties(annotations, column, insertable, updatable);
	}


	public Iterator<Property> getToStringPropertiesIterator() {
		Iterator<Property> iter = getAllPropertiesIterator();
		return new ToStringHelper(c2j).findToStringProperties(iter);

	}

	public Iterator<Property> getEqualsHashCodePropertiesIterator() {
		Iterator<Property> iter = getAllPropertiesIterator();
		return new EqualsAndHashCodeHelper(c2j, this, false).findProperties(iter);
	}

	public boolean needsToString() {
		Iterator<Property> iter = getAllPropertiesIterator();
		return new ToStringHelper(c2j).needsToString(iter);
	}

	public boolean hasMetaAttribute(MetaAttributable pc, String attribute) {
		return pc.getMetaAttribute( attribute ) != null;
	}

	public boolean getMetaAttribAsBool(MetaAttributable pc, String attribute, boolean defaultValue) {
		return MetaAttributeHelper.getMetaAsBool( pc.getMetaAttribute( attribute ), defaultValue );
	}
	
	public boolean hasFieldJavaDoc(Property property) {
		return property.getMetaAttribute("field-description")!=null;
	}
	
	public String getFieldJavaDoc(Property property, int indent) {
		MetaAttribute descriptor = property.getMetaAttribute( "field-description" );
		if ( descriptor == null ) {
			return c2j.toJavaDoc( "", indent );
		}

		return c2j.toJavaDoc( c2j.getMetaAsString( property, "field-description" ), indent );
	}
	
	public String getFieldDescription(Property property){
		MetaAttribute descriptor = property.getMetaAttribute( "field-description" );
		if ( descriptor == null ) {
			return "";
		}

		return c2j.getMetaAsString( property, "field-description" );
	}

	/**
	 * Method getGetterSignature.
	 *
	 * @return String
	 */
	public String getGetterSignature(Property p) {
		String prefix = c2j.getJavaTypeName( p, false).equals( "boolean" ) ? "is" : "get";
		return prefix + beanCapitalize( p.getName() );
	}

	/**
	 * @param p
	 * @return foo -> Foo, FOo -> FOo
	 */
	public String getPropertyName(Property p) {
		return beanCapitalize( p.getName() );
	}


	// get the "opposite" collectionnae for a property. Currently a "hack" that just uses the same naming algorithm as in reveng, will fail on more general models!
	public String getCollectionNameFor(Property property) {
		String str = getPropertyName(property);
		return NameConverter.simplePluralize(str);
	}
	
	
	/**
	 * foo -> Foo
	 * FOo -> FOo
	 */
	static public String beanCapitalize(String fieldName) {
		return BeanUtils.capitalize(fieldName);
	}


	public boolean isComponent(Property property) {
		Value value = property.getValue();

		return value instanceof Component;
	}

	public String generateHashCode(Property property, String result, String thisName, boolean jdk5) {
		return new EqualsAndHashCodeHelper(c2j, this, false).generateHashCode(property, result, thisName, jdk5);
	}

	public String getFieldModifiers(Property property) {
		return BeanUtils.getModifiers( property, "scope-field", "private" );
	}

	public String getPropertyGetModifiers(Property property) {
		return BeanUtils.getModifiers( property, "scope-get", "public" );
	}

	public String getPropertySetModifiers(Property property) {
		return BeanUtils.getModifiers( property, "scope-set", "public" );
	}

	protected boolean isRequiredInConstructor(Property field) {
		return BeanUtils.requiredInConstructor(field, this);
	}

	public boolean needsMinimalConstructor() {
		List<Property> propClosure = getPropertyClosureForMinimalConstructor();
		if(propClosure.isEmpty()) {
			return false; // minimal=default
		}

		return !propClosure.equals(getPropertyClosureForFullConstructor()); // minimal=full
	}

	public boolean needsFullConstructor() {
		return !getPropertyClosureForFullConstructor().isEmpty();		
	}
	
	public String getJavaTypeName(Property p, boolean useGenerics) {
		return c2j.getJavaTypeName(p, useGenerics, this);
	}

	public boolean hasFieldInitializor(Property p, boolean useGenerics) {
		return getFieldInitialization(p, useGenerics)!= null;
	}
	
	public String getFieldInitialization(Property property, boolean useGenerics) {
		return new FieldsInitializer(c2j, this, importContext).findInitializerTo(property, useGenerics);

	}	
	
}
 
