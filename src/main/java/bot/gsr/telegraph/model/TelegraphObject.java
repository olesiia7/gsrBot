package bot.gsr.telegraph.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface TelegraphObject extends Serializable {
}