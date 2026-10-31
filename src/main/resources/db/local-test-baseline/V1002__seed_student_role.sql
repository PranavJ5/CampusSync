INSERT INTO `role` (role_code, role_name, description)
SELECT 'STUDENT', 'Student', 'Student account role'
WHERE NOT EXISTS (
    SELECT 1 FROM `role`
    WHERE role_code = 'STUDENT'
);
