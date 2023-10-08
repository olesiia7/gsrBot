package bot.gsr.telegram.model;

import bot.gsr.SQLite.model.Category;

public record CategorySummary(Category category, int count, int priceSum) {
}
