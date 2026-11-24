package com.linan.core.plugin;

import java.util.List;
import java.util.Map;

/**
 * 插件清单
 *
 * 插件的元数据描述文件，包含插件的基本信息、依赖关系、配置项等
 *
 * @author BareZen Team
 */
public class PluginManifest {

    private String pluginId;
    private String pluginName;
    private String version;
    private String description;
    private String author;
    private List<String> dependencies;
    private Map<String, Object> configuration;
    private List<String> controllers;
    private List<String> services;
    private List<String> entities;
    private List<String> migrations;
    private List<Map<String, Object>> frontendRoutes;
    private List<String> permissions;

    // Getters and Setters

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public List<String> getControllers() {
        return controllers;
    }

    public void setControllers(List<String> controllers) {
        this.controllers = controllers;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public List<String> getEntities() {
        return entities;
    }

    public void setEntities(List<String> entities) {
        this.entities = entities;
    }

    public List<String> getMigrations() {
        return migrations;
    }

    public void setMigrations(List<String> migrations) {
        this.migrations = migrations;
    }

    public List<Map<String, Object>> getFrontendRoutes() {
        return frontendRoutes;
    }

    public void setFrontendRoutes(List<Map<String, Object>> frontendRoutes) {
        this.frontendRoutes = frontendRoutes;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}