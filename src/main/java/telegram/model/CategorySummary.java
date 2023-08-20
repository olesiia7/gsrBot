package telegram.model;

import SQLite.model.Category;

public record CategorySummary(Category category, int count, int priceSum) {
}
