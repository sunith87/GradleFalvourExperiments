class BuildProperties {

    private final Properties properties
    private final File parent
    private final BuildProperties defaults
    private final def project

    public BuildProperties(File path, def project) {
        this.project = project
        this.parent = path.getParentFile()
        this.properties = new Properties()
        this.properties.load(new FileInputStream(path))
        if (properties.getProperty('extends') != null) {
            this.defaults = getProperties('extends')
        } else {
            this.defaults = null
        }
    }

    void configure(def flavor) {
        println ">>> Configuring flavor \"${flavor.name}\" using ${getAbsolutePath()}"

        flavor.buildConfigField "String", "VERSION_CODE_NAME", "\"${getString('versionCodeName')}\""
        flavor.buildConfigField "String", "API_VERSION", "\"${getString('apiVersion')}\""
        flavor.buildConfigField "String", "ANALYTICS_KEY", "\"${getString('analyticsKey')}\""

        configureCountryProps(flavor)
        configureEnvironmentProps(flavor)
        configureCurrencyProps(flavor)

    }

    void configureCountryProps(def flavor) {
        addConfigFieldIfPresent(flavor, "String", "COUNTRY_CODE", "countryCode")
    }

    void configureEnvironmentProps(def flavor) {
        addConfigFieldIfPresent(flavor, "String", "URL", "url")
    }

    void configureCurrencyProps(def flavor) {
        addConfigFieldIfPresent(flavor, "String", "CURRENCY_SYMBOL", "currencySymbol")
    }


    void addConfigFieldIfPresent(def flavor, String type, String field, String value) {
        if (contains(value)) {
            if (type.equals("String")) {
                flavor.buildConfigField type, field, "\"${getString(value)}\""
            } else {
                flavor.buildConfigField type, field, "${getString(value)}"
            }
        }
    }


    int getInt(String property) {
        checkPropertyOrThrow(property)
        Integer.parseInt(getPropertyValue(property))
    }

    String getString(String property) {
        checkPropertyOrThrow(property)
        getPropertyValue(property)
    }

    File getFile(String property) {
        String filePath = getString(property)
        File file = new File(filePath)
        if (file.exists()) {
            file
        } else {
            new File(parent, filePath)
        }
    }

    BuildProperties getProperties(String property) {
        checkPropertyOrThrow(property)
        new BuildProperties(getFile(property), project)
    }

    private void checkPropertyOrThrow(String property) {
        if (getPropertyValue(property) == null) {
            throw new RuntimeException("Please define correct value for \"${property}\"")
        }
    }

    boolean contains(String name) {
        return getPropertyValue(name)
    }

    private String getPropertyValue(String name) {
        println("Name = "+name)

        if (project.hasProperty(name)) {
            return project.getProperty(name)
        }

        if (defaults != null) {
            return properties.getProperty(name, defaults.getPropertyValue(name))
        }

        return properties.getProperty(name, null)
    }

    String getAbsolutePath() {
        parent.getAbsolutePath()
    }

    boolean equals(o) {
        if (this.is(o)) {
            return true
        }

        if (!(o instanceof BuildProperties)) {
            return false
        }

        BuildProperties that = (BuildProperties) o
        parent.equals(that.parent)
    }


    static BuildProperties load(String path, def project) {
        String fullPath
        if (path.startsWith("/")) {
            File rootDir = project.projectDir
            fullPath = rootDir.getParent() + path
        } else {
            fullPath = path
        }
        File file = new File(fullPath)
        if (!file.exists()) {
            throw new RuntimeException("Error property file ${file.getAbsolutePath()} not found")
        }
        new BuildProperties(file, project)
    }


}
