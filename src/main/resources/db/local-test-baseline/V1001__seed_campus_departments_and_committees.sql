UPDATE department SET is_active = 1
WHERE department_name = 'Computer Engineering';

INSERT INTO department (department_code, department_name, is_active)
SELECT 'COMPUTER_ENGINEERING', 'Computer Engineering', 1
WHERE NOT EXISTS (
    SELECT 1 FROM department
    WHERE department_name = 'Computer Engineering'
);

UPDATE department SET is_active = 1
WHERE department_name = 'Computer Science and Engineering';

INSERT INTO department (department_code, department_name, is_active)
SELECT 'COMPUTER_SCIENCE_AND_ENGINEERING', 'Computer Science and Engineering', 1
WHERE NOT EXISTS (
    SELECT 1 FROM department
    WHERE department_name = 'Computer Science and Engineering'
);

UPDATE department SET is_active = 1
WHERE department_name = 'Electronics and Telecommunication';

INSERT INTO department (department_code, department_name, is_active)
SELECT 'ELECTRONICS_AND_TELECOMMUNICATION', 'Electronics and Telecommunication', 1
WHERE NOT EXISTS (
    SELECT 1 FROM department
    WHERE department_name = 'Electronics and Telecommunication'
);

UPDATE committee SET is_active = 1, status = 'ACTIVE'
WHERE committee_name = 'Sports Committee';

INSERT INTO committee (
    department_id, committee_code, committee_name, category, is_active, status
)
SELECT NULL, 'SPORTS', 'Sports Committee', 'SPORTS', 1, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM committee
    WHERE committee_name = 'Sports Committee'
);

UPDATE committee SET is_active = 1, status = 'ACTIVE'
WHERE committee_name = 'Oculus Committee';

INSERT INTO committee (
    department_id, committee_code, committee_name, category, is_active, status
)
SELECT NULL, 'OCULUS', 'Oculus Committee', 'CULTURAL', 1, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM committee
    WHERE committee_name = 'Oculus Committee'
);

UPDATE committee SET is_active = 1, status = 'ACTIVE'
WHERE committee_name = 'E-Cell';

INSERT INTO committee (
    department_id, committee_code, committee_name, category, is_active, status
)
SELECT NULL, 'E_CELL', 'E-Cell', 'ENTREPRENEURSHIP', 1, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM committee
    WHERE committee_name = 'E-Cell'
);

UPDATE committee SET is_active = 1, status = 'ACTIVE'
WHERE committee_name = 'Rotaract';

INSERT INTO committee (
    department_id, committee_code, committee_name, category, is_active, status
)
SELECT NULL, 'ROTARACT', 'Rotaract', 'SERVICE', 1, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM committee
    WHERE committee_name = 'Rotaract'
);

UPDATE committee SET is_active = 1, status = 'ACTIVE'
WHERE committee_name = 'IEEE CS';

INSERT INTO committee (
    department_id, committee_code, committee_name, category, is_active, status
)
SELECT NULL, 'IEEE_CS', 'IEEE CS', 'TECHNICAL', 1, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM committee
    WHERE committee_name = 'IEEE CS'
);
