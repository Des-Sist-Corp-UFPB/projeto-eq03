-- Rename the service table to avoid confusion with Spring "Service" component and general naming collision
ALTER TABLE tb_service RENAME TO tb_salon_service;

-- Rename the foreign key column in appointment table
ALTER TABLE tb_appointment RENAME COLUMN service_id TO salon_service_id;
