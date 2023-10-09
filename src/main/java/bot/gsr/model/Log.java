package bot.gsr.model;


import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.sql.Date;

public record Log(@NotNull Date date,
                  @NotNull String description,
                  int price,
                  @NotNull Category category,
                  @Nullable SessionType sessionType) {
}