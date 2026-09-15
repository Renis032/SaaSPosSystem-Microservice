package com.renko.service.impl;

import com.renko.service.DevCleanupService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DevCleanupServiceImpl implements DevCleanupService
{
    private static final Set<String> PRESERVE_TABLES = Set.of(
            "flyway_schema_history"
    );

    /**
     * Hibernate SEQUENCE/TABLE generators (GenerationType.AUTO on MySQL) use a pooled
     * optimizer with allocation size 50. Seeding next_val=1 makes the first "high" value 1,
     * so IDs start around -49. Seed the high value to the allocation size instead.
     */
    private static final long HIBERNATE_SEQUENCE_INITIAL_HIGH = 50L;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Must run and commit in its own transaction. Truncating sequence tables inside
     * the same open Hibernate transaction that later allocates IDs causes lock waits
     * ("Error performing isolated work").
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<String> clearAllTables()
    {
        entityManager.flush();
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

        @SuppressWarnings("unchecked")
        List<Object> tables = entityManager.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'"
        ).getResultList();

        List<String> truncated = new ArrayList<>();
        List<String> sequenceTables = new ArrayList<>();

        for (Object row : tables)
        {
            String table = row == null ? null : String.valueOf(row);
            if (table == null || table.isBlank() || "null".equalsIgnoreCase(table))
            {
                continue;
            }
            if (PRESERVE_TABLES.contains(table.toLowerCase(Locale.ROOT)))
            {
                continue;
            }

            entityManager.createNativeQuery("TRUNCATE TABLE `" + table + "`").executeUpdate();
            truncated.add(table);

            if (isSequenceTable(table))
            {
                sequenceTables.add(table);
            }
        }

        // Keep *_seq rows valid if any AUTO/sequence generators remain.
        for (String sequenceTable : sequenceTables)
        {
            entityManager.createNativeQuery(
                    "INSERT INTO `" + sequenceTable + "` (next_val) VALUES (" + HIBERNATE_SEQUENCE_INITIAL_HIGH + ")"
            ).executeUpdate();
        }

        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        entityManager.clear();
        return truncated;
    }

    private static boolean isSequenceTable(String table)
    {
        String name = table.toLowerCase(Locale.ROOT);
        return name.endsWith("_seq") || "hibernate_sequence".equals(name);
    }
}
