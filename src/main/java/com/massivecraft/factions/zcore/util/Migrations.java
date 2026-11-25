package com.massivecraft.factions.zcore.util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Numbered data-format migrations. Version 1 is the initial versioned layout;
 * the JSON files themselves are unchanged from the pre-versioned format.
 *
 * <p>Every future format change must increment {@link #CURRENT_VERSION} and
 * add a case here. A snapshot is created before this method is called.</p>
 *
 * @author MarcDoesntExists
 */
final class Migrations {

    static final int CURRENT_VERSION = 2;

    private Migrations() {
    }

    static void migrate(Path dataFolder, int fromVersion, int toVersion) throws IOException {
        for (int target = fromVersion + 1; target <= toVersion; target++) {
            switch (target) {
                case 1 -> {
                    // Initial versioned format: the existing JSON layout is kept.
                }
                case 2 -> {
                    // Per-file versions (bank journal, idempotency, mailbox) are
                    // migrated by the owning stores after the folder snapshot.
                }
                default -> throw new IOException(
                        "No migration registered for data version " + target);
            }
        }
    }
}
