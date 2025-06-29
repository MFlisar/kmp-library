{% if project["library"]["multiplatform"] %}

{% if project["modules"] | length == 1 %}
This is a **KMP (kotlin multiplatform)** library which supports following platforms.
{% else %}
This is a **KMP (kotlin multiplatform)** library and the provided modules do support following platforms.
{% endif %}

<table>
  <tr>
    {% if project["modules"] is defined and project["modules"] | length > 1 %}
    <th>Module</th>
    {% endif %}
	{% for platform in project["library"]["platforms"] %}
    <th>{{ platform["name"] }}</th>
	{% endfor %}
    <th>Info</th>
  </tr>

{% if project["modules"] | length == 1 %}

  <tr>
      {% for platform in project["library"]["platforms"] %}
		<td>
		  {% if platform["name"] in project["modules"][0]["platforms"] %}✔{% else %}-{% endif %}
		</td>
	  {% endfor %}
      <td>
        {{ project["modules"][0]["platform-info"] }}
      </td>
  </tr>

{% elif project["modules"] is defined %}

{% for group in project["groups"] %}

    <tr><td colspan="{{ project["library"]["platforms"] | length + 2 }}" style="background-color:var(--md-primary-fg-color--light);">{{ group["label"] }}</td></tr>

    {% for module in project["modules"] %}
      {% if module["group"] == group["id"] %}
          
        <tr>
            <td><code>{{ module["id"] }}</code></td>
            {% for platform in project["library"]["platforms"] %}
				<td>
				  {% if platform["name"] in module["platforms"] %}✔{% else %}-{% endif %}
				</td>
			{% endfor %}
            <td>
              {{ module["platform-info"] }}
            </td>
        </tr>
          
      {% endif %}
    {% endfor %}

{% endfor %}

{% endif %}

</table>

{% endif %}