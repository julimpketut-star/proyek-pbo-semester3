package com.studiomusik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AppInfoTest {

    @Test
    void appNameMatchesProjectContract() {
        assertEquals("Sistem Pengelolaan Operasional Studio Musik", AppInfo.APP_NAME);
    }

    @Test
    void versionIsNotBlankAndFollowsSnapshotConvention() {
        assertFalse(AppInfo.VERSION.isBlank());
        assertTrue(AppInfo.VERSION.endsWith("-SNAPSHOT"));
    }

    @Test
    void basePackageMatchesActualPackage() {
        assertEquals(AppInfo.class.getPackageName(), AppInfo.BASE_PACKAGE);
    }

    @Test
    void databaseNameAndJavaReleaseMatchContract() {
        assertEquals("studio_musik", AppInfo.DATABASE_NAME);
        assertEquals(21, AppInfo.JAVA_RELEASE);
    }

    @Test
    void windowTitleContainsNameAndVersion() {
        String title = AppInfo.getWindowTitle();
        assertTrue(title.contains(AppInfo.APP_NAME));
        assertTrue(title.endsWith("v" + AppInfo.VERSION));
    }
}
