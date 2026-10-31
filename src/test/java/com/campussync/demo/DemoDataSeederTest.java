package com.campussync.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class DemoDataSeederTest {
    @Test
    void committeeSeedMatchesTheFiveCanonicalLocalTestReferencesAndUsesSyntheticAccounts() {
        var seeds = DemoDataSeeder.committeeSeeds();

        assertEquals(Set.of("SPORTS", "OCULUS", "E_CELL", "ROTARACT", "IEEE_CS"),
                seeds.stream().map(DemoDataSeeder.CommitteeSeed::code).collect(Collectors.toSet()));
        assertEquals(Set.of("Sports Committee", "Oculus Committee", "E-Cell", "Rotaract", "IEEE CS"),
                seeds.stream().map(DemoDataSeeder.CommitteeSeed::name).collect(Collectors.toSet()));
        assertEquals(Set.of("SPORTS", "CULTURAL", "ENTREPRENEURSHIP", "SERVICE", "TECHNICAL"),
                seeds.stream().map(DemoDataSeeder.CommitteeSeed::category).collect(Collectors.toSet()));
        assertTrue(seeds.stream().allMatch(seed -> seed.facultyUsername().matches("demo\\.faculty0[1-8]")));
        assertEquals(8, seeds.stream().flatMap(seed -> seed.studentUsernames().stream()).distinct().count());
        assertTrue(seeds.stream().flatMap(seed -> seed.studentUsernames().stream())
                .allMatch(username -> username.matches("demo\\.student0[1-8]")));
    }

    @Test
    void committeeReferenceModeAcceptsOnlyTheTwoRequiredInsertGrants() {
        DemoDataSeeder.validateSeederGrants(List.of(
                "GRANT USAGE ON *.* TO `campussync_demo_seed`@`localhost`",
                "GRANT SELECT ON `campussync_demo`.* TO `campussync_demo_seed`@`localhost`",
                "GRANT INSERT ON `campussync_demo`.`committee` TO `campussync_demo_seed`@`localhost`",
                "GRANT INSERT ON `campussync_demo`.`committee_member` TO `campussync_demo_seed`@`localhost`"),
                DemoDataSeeder.COMMITTEE_REFERENCE_INSERT_TABLES);
    }

    @Test
    void fullSeedStillRequiresEveryApprovedInsertGrant() {
        List<String> referenceOnlyGrants = List.of(
                "GRANT SELECT ON campussync_demo.* TO 'campussync_demo_seed'@'localhost'",
                "GRANT INSERT ON campussync_demo.committee TO 'campussync_demo_seed'@'localhost'",
                "GRANT INSERT ON campussync_demo.committee_member TO 'campussync_demo_seed'@'localhost'");

        assertThrows(IllegalStateException.class, () -> DemoDataSeeder.validateSeederGrants(
                referenceOnlyGrants,
                Set.of("role", "department", "user", "student", "faculty", "subject",
                        "timetable_entry", "committee", "committee_member")));
    }

    @Test
    void committeeReferenceModeRejectsAdditionalPrivilegesAndOutOfScopeGrants() {
        List<String> validReferenceGrants = List.of(
                "GRANT SELECT ON campussync_demo.* TO 'campussync_demo_seed'@'localhost'",
                "GRANT INSERT ON campussync_demo.committee TO 'campussync_demo_seed'@'localhost'",
                "GRANT INSERT ON campussync_demo.committee_member TO 'campussync_demo_seed'@'localhost'");

        assertThrows(IllegalStateException.class, () -> DemoDataSeeder.validateSeederGrants(
                append(validReferenceGrants,
                        "GRANT UPDATE ON campussync_demo.committee TO 'campussync_demo_seed'@'localhost'"),
                DemoDataSeeder.COMMITTEE_REFERENCE_INSERT_TABLES));
        assertThrows(IllegalStateException.class, () -> DemoDataSeeder.validateSeederGrants(
                append(validReferenceGrants, "GRANT INSERT ON *.* TO 'campussync_demo_seed'@'localhost'"),
                DemoDataSeeder.COMMITTEE_REFERENCE_INSERT_TABLES));
        assertThrows(IllegalStateException.class, () -> DemoDataSeeder.validateSeederGrants(
                validReferenceGrants.subList(0, 2), DemoDataSeeder.COMMITTEE_REFERENCE_INSERT_TABLES));
    }

    @Test
    void committeeReferenceModeUsesExistingUniqueKeyAfterConcurrentInsert() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement initialLookup = mock(PreparedStatement.class);
        PreparedStatement insert = mock(PreparedStatement.class);
        PreparedStatement concurrentLookup = mock(PreparedStatement.class);
        ResultSet noExistingCommittee = mock(ResultSet.class);
        ResultSet concurrentCommittee = committeeRow();
        when(connection.prepareStatement(anyString()))
                .thenReturn(initialLookup, concurrentLookup);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(insert);
        when(initialLookup.executeQuery()).thenReturn(noExistingCommittee);
        when(noExistingCommittee.next()).thenReturn(false);
        when(insert.executeUpdate()).thenThrow(new SQLException("duplicate key", "23000", 1062));
        when(concurrentLookup.executeQuery()).thenReturn(concurrentCommittee);

        long id = DemoDataSeeder.findOrVerifyCommittee(
                connection, sportsCommittee(), 42L);

        assertEquals(9L, id);
        verify(insert).executeUpdate();
        verify(connection, times(2)).prepareStatement("SELECT committee_id, committee_code, committee_name, category, "
                + "faculty_coordinator_id, is_active, status FROM committee WHERE committee_code = ? "
                + "OR committee_name = ?");
    }

    @Test
    void committeeReferenceModeLeavesMatchingCommitteeUnchanged() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement lookup = mock(PreparedStatement.class);
        ResultSet existingCommittee = committeeRow();
        when(connection.prepareStatement(anyString())).thenReturn(lookup);
        when(lookup.executeQuery()).thenReturn(existingCommittee);

        assertEquals(9L, DemoDataSeeder.findOrVerifyCommittee(connection, sportsCommittee(), 42L));

        verify(connection).prepareStatement(anyString());
    }

    @Test
    void committeeReferenceModeUsesExistingUniqueKeyForConcurrentMembership() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement initialLookup = mock(PreparedStatement.class);
        PreparedStatement insert = mock(PreparedStatement.class);
        PreparedStatement concurrentLookup = mock(PreparedStatement.class);
        ResultSet noMembership = mock(ResultSet.class);
        ResultSet concurrentMembership = mock(ResultSet.class);
        when(connection.prepareStatement(anyString()))
                .thenReturn(initialLookup, insert, concurrentLookup);
        when(initialLookup.executeQuery()).thenReturn(noMembership);
        when(noMembership.next()).thenReturn(false);
        when(insert.executeUpdate()).thenThrow(new SQLException("duplicate key", "23000", 1062));
        when(concurrentLookup.executeQuery()).thenReturn(concurrentMembership);
        when(concurrentMembership.next()).thenReturn(true, false);
        when(concurrentMembership.getString("member_role")).thenReturn("COORDINATOR");

        DemoDataSeeder.ensureCoordinatorMembership(connection, 9L, 24L);

        verify(insert).executeUpdate();
        ArgumentCaptor<String> sql = forClass(String.class);
        verify(connection, org.mockito.Mockito.atLeastOnce()).prepareStatement(sql.capture());
        assertTrue(sql.getAllValues().stream().anyMatch(query -> query.contains("FROM committee_member")));
        assertFalse(sql.getAllValues().stream().anyMatch(query -> query.toUpperCase().contains("FOR UPDATE")));
    }

    @Test
    void committeeReferenceModeLeavesExistingCoordinatorMembershipUnchanged() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement lookup = mock(PreparedStatement.class);
        ResultSet membership = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(lookup);
        when(lookup.executeQuery()).thenReturn(membership);
        when(membership.next()).thenReturn(true, false);
        when(membership.getString("member_role")).thenReturn("COORDINATOR");

        DemoDataSeeder.ensureCoordinatorMembership(connection, 9L, 24L);

        verify(connection).prepareStatement(anyString());
    }

    private static ResultSet committeeRow() throws SQLException {
        ResultSet row = mock(ResultSet.class);
        when(row.next()).thenReturn(true, false);
        when(row.getLong("committee_id")).thenReturn(9L);
        when(row.getObject("faculty_coordinator_id", Long.class)).thenReturn(42L);
        when(row.getString("committee_code")).thenReturn("SPORTS");
        when(row.getString("committee_name")).thenReturn("Sports Committee");
        when(row.getString("category")).thenReturn("SPORTS");
        when(row.getBoolean("is_active")).thenReturn(true);
        when(row.getString("status")).thenReturn("ACTIVE");
        return row;
    }

    private static DemoDataSeeder.CommitteeSeed sportsCommittee() {
        return new DemoDataSeeder.CommitteeSeed(
                "SPORTS", "Sports Committee", "SPORTS", "demo.faculty01", List.of());
    }

    private static List<String> append(List<String> grants, String extraGrant) {
        List<String> result = new ArrayList<>(grants);
        result.add(extraGrant);
        return result;
    }
}
