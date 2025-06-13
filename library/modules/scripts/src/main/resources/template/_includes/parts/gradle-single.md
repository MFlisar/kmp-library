{% if project["modules"] is not defined or project["modules"] | length == 1 %}

=== "Dependencies"

    Simply add the dependencies inside your `build.gradle.kts` file.

    ```kotlin title="build.gradle.kts"

    val {{ project["library"]["id"] }} = "<LATEST-VERSION>"

    implementation("{{ project["library"]["maven"] }}:{{ project["library"]["maven-main-library"] }}:${{ project["library"]["id"] }}")
    ```

=== "Version Catalog"

    Define the dependencies inside your `libs.versions.toml` file.

    ```toml title="libs.versions.toml"
    [versions]
    {{ project["library"]["id"] }} = "<LATEST-VERSION>"
    
    [libraries]
   
    {%- set module = project["library"]["maven-main-library"] -%}
    {%- set name = project["library"]["id"] ~ " =" -%}
    {% set module2 = "\"" ~ project["library"]["maven"] ~ ":" ~ module ~ "\"," %}
    {{ name }} { module = {{ module2 }} version.ref = "{{ project["library"]["id"] }}" }
    ```

    And then use the definitions in your projects like following:

    ```kotlin title="build.gradle.kts"
    implementation(libs.{{ project["library"]["id"] }})
    ```

{% endif %}