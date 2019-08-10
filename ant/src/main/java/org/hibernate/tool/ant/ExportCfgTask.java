package org.hibernate.tool.ant;

import java.io.File;
import java.util.Properties;

import org.apache.tools.ant.types.Environment.Variable;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;

public class ExportCfgTask {
	
	HibernateToolTask parent = null;
	Properties properties = new Properties();
	
	public ExportCfgTask(HibernateToolTask parent) {
		this.parent = parent;
	}
	
	public Properties getProperties() {
		return this.properties;
	}
	
	public void setDestinationFolder(File destinationFolder) {
		this.properties.put(ExporterConstants.DESTINATION_FOLDER, destinationFolder);
	}
	
	public void setMetadataDescriptor(MetadataDescriptor metadataDescriptor) {
		this.properties.put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
	}
	
	public void addConfiguredProperty(Variable variable) {
		properties.put(variable.getKey(), variable.getValue());
	}
	
	public void execute() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.CFG);
		exporter.getProperties().putAll(this.properties);
		exporter.start();
	}

}
