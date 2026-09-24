-- H2 test schema - no CHECK constraints on enums (H2 doesn't support Hibernate 7 enum check syntax)

DROP TABLE IF EXISTS incident_comments;
DROP TABLE IF EXISTS incident_resolution_cycles;
DROP TABLE IF EXISTS incident_histories;
DROP TABLE IF EXISTS incidents;
DROP TABLE IF EXISTS incident_type_configs;

CREATE TABLE incident_type_configs (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sla_hours INTEGER DEFAULT 48,
    default_target_service_id UUID,
    default_target_user_id UUID,
    requires_validation BOOLEAN NOT NULL DEFAULT TRUE,
    requires_cause_analysis BOOLEAN NOT NULL DEFAULT TRUE,
    email_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    in_app_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    validator_scope VARCHAR(50) NOT NULL DEFAULT 'SOURCE_SERVICE_MANAGER',
    default_criticality VARCHAR(50) DEFAULT 'HIGH',
    requires_direction_validation BOOLEAN NOT NULL DEFAULT TRUE,
    direction_validator_id UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    modified_by UUID
);

DROP TABLE IF EXISTS incident_type_config_treater_roles;
DROP TABLE IF EXISTS incident_type_config_resolver_roles;
DROP TABLE IF EXISTS incident_type_config_closer_roles;
DROP TABLE IF EXISTS incident_type_config_reopener_roles;

CREATE TABLE incident_type_config_treater_roles (
    incident_type_config_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL
);
CREATE TABLE incident_type_config_resolver_roles (
    incident_type_config_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL
);
CREATE TABLE incident_type_config_closer_roles (
    incident_type_config_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL
);
CREATE TABLE incident_type_config_reopener_roles (
    incident_type_config_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    type_id UUID NOT NULL,
    criticality VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    validated_at TIMESTAMP,
    transferred_at TIMESTAMP,
    resolved_at TIMESTAMP,
    closed_at TIMESTAMP,
    blocked_at TIMESTAMP,
    unblocked_at TIMESTAMP,
    reopened_at TIMESTAMP,
    due_date TIMESTAMP,
    initial_due_date TIMESTAMP,
    created_by UUID NOT NULL,
    validated_by UUID,
    assigned_to UUID,
    blocked_by UUID,
    reopened_by UUID,
    transferred_to_service UUID,
    initial_target_service_id UUID,
    transfer_reason VARCHAR(5000),
    reject_reason VARCHAR(5000),
    cancel_reason VARCHAR(5000),
    blocked_reason VARCHAR(5000),
    reopen_reason VARCHAR(5000),
    reopen_count INTEGER NOT NULL DEFAULT 0,
    cause VARCHAR(50),
    cause_detail VARCHAR(2000),
    reference VARCHAR(20),
    treatment_description VARCHAR(5000),
    resolution_description VARCHAR(5000),
    closure_description VARCHAR(5000),
    incident_date DATE,
    observation_date DATE,
    agency_id UUID NOT NULL,
    creator_service_id UUID,
    source_incident_id UUID,
    last_sla_reminder_sent_at TIMESTAMP,
    decision_awaited_since TIMESTAMP,
    last_decision_reminder_sent_at TIMESTAMP,
    confirmation_requested_at TIMESTAMP,
    confirmation_requested_by UUID,
    last_confirmation_reminder_sent_at TIMESTAMP,
    last_critical_reminder_sent_at TIMESTAMP,
    proposed_solution VARCHAR(15000),
    direction_rejection_reason VARCHAR(5000),
    estimated_resolution_hours INTEGER,
    pre_block_status VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    modified_by UUID
);

CREATE TABLE incident_histories (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id),
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    comment VARCHAR(1000),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    modified_by UUID
);

CREATE TABLE incident_comments (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id),
    user_id UUID NOT NULL,
    content VARCHAR(2000) NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    parent_comment_id UUID REFERENCES incident_comments(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    modified_by UUID
);

CREATE INDEX idx_incidents_status_created_at ON incidents(status, created_at);
CREATE INDEX idx_incidents_agency_created_at ON incidents(agency_id, created_at);
CREATE INDEX idx_incidents_assigned_status ON incidents(assigned_to, status);
CREATE INDEX idx_incidents_created_by_created_at ON incidents(created_by, created_at);
CREATE INDEX idx_incidents_creator_service_created_at ON incidents(creator_service_id, created_at);
CREATE INDEX idx_incidents_transferred_service_created_at ON incidents(transferred_to_service, created_at);
CREATE INDEX idx_incidents_due_status ON incidents(due_date, status);

CREATE TABLE incident_resolution_cycles (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id),
    cycle_no INTEGER NOT NULL,
    started_at TIMESTAMP NOT NULL,
    validated_at TIMESTAMP,
    treated_at TIMESTAMP,
    resolved_at TIMESTAMP,
    closed_at TIMESTAMP,
    due_date_snapshot TIMESTAMP,
    outcome VARCHAR(30) NOT NULL,
    paused_minutes BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    modified_by UUID,
    CONSTRAINT uk_incident_resolution_cycle_no UNIQUE (incident_id, cycle_no)
);

CREATE INDEX idx_incident_cycle_closed_at ON incident_resolution_cycles(closed_at);
CREATE INDEX idx_incident_cycle_resolved_at ON incident_resolution_cycles(resolved_at);

CREATE TABLE IF NOT EXISTS incident_reference_sequences (
    reference_year INT PRIMARY KEY,
    last_number BIGINT NOT NULL
);
