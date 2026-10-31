INSERT INTO `role` (role_code, role_name, description)
SELECT 'ADMINISTRATOR', 'Administrator', 'Global campus administration role'
WHERE NOT EXISTS (
    SELECT 1 FROM `role`
    WHERE role_code = 'ADMINISTRATOR'
);

INSERT INTO `role` (role_code, role_name, description)
SELECT 'FACULTY', 'Faculty', 'Faculty account role'
WHERE NOT EXISTS (
    SELECT 1 FROM `role`
    WHERE role_code = 'FACULTY'
);

INSERT INTO `role` (role_code, role_name, description)
SELECT 'COMMITTEE_COORDINATOR', 'Committee Coordinator',
       'Coordinator account role scoped to active committee memberships'
WHERE NOT EXISTS (
    SELECT 1 FROM `role`
    WHERE role_code = 'COMMITTEE_COORDINATOR'
);
