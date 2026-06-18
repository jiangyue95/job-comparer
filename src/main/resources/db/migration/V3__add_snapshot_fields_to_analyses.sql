-- V3: Add snapshot fields to analyses
-- add three fields: cv_name, job_title and company

ALTER TABLE analyses ADD COLUMN cv_name VARCHAR(255);
ALTER TABLE analyses ADD COLUMN job_title VARCHAR(255);
ALTER TABLE analyses ADD COLUMN company VARCHAR(255);