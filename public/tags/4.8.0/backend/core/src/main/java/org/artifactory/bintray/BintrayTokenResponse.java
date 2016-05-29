package org.artifactory.bintray;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.plexus.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dan Feldman
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BintrayTokenResponse {

    @JsonProperty(value = "access_token")
    public String token;
    @JsonProperty(value = "refresh_token")
    public String refreshToken;

    //Everything else we might care about but currently don't
    private Map<String, Object> other = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> any() {
        return other;
    }

    @JsonAnySetter
    public void set(String name, Object value) {
        other.put(name, value);
    }

    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isNotBlank(token) && StringUtils.isNotBlank(refreshToken);
    }
}
