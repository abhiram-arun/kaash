package dev.expensetracker.data

import dev.expensetracker.ui.ICONS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxonomyTest {
    @Test fun everyIconKeyIsMapped() {
        val keys = TAXONOMY.flatMap { t -> listOf(t.icon) + t.subs.map { it.second } }
        val missing = keys.filter { it !in ICONS }
        assertTrue("Unmapped icon keys: $missing", missing.isEmpty())
    }

    // seedCategories matches by name, so duplicates would be silently merged.
    @Test fun namesAreUnique() {
        assertEquals(TAXONOMY.size, TAXONOMY.map { it.name }.toSet().size)
        TAXONOMY.forEach { t -> assertEquals(t.name, t.subs.size, t.subs.map { it.first }.toSet().size) }
    }

    // MIGRATION_1_2 renames v1 categories; each target must exist so old tags land on a real topic.
    @Test fun v1CategoriesMapToTopics() {
        listOf("Food & Dining", "Groceries", "Transport", "Housing", "Bills & Utilities", "Shopping", "Transfers", "Income", "Other")
            .forEach { assertTrue(it, it in TOPIC_ORDER) }
    }
}
