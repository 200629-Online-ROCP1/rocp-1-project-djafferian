-- DROP SCHEMA banking;

CREATE SCHEMA banking AUTHORIZATION postgres;

COMMENT ON SCHEMA banking IS '200629-Online-ROCP1';

-- DROP TYPE banking."_account";

CREATE TYPE banking."_account" (
	INPUT = array_in,
	OUTPUT = array_out,
	RECEIVE = array_recv,
	SEND = array_send,
	ANALYZE = array_typanalyze,
	ALIGNMENT = 8,
	STORAGE = any,
	CATEGORY = A,
	ELEMENT = banking.account,
	DELIMITER = ',');

-- DROP TYPE banking."_account_status";

CREATE TYPE banking."_account_status" (
	INPUT = array_in,
	OUTPUT = array_out,
	RECEIVE = array_recv,
	SEND = array_send,
	ANALYZE = array_typanalyze,
	ALIGNMENT = 8,
	STORAGE = any,
	CATEGORY = A,
	ELEMENT = banking.account_status,
	DELIMITER = ',');

-- DROP TYPE banking."_account_type";

CREATE TYPE banking."_account_type" (
	INPUT = array_in,
	OUTPUT = array_out,
	RECEIVE = array_recv,
	SEND = array_send,
	ANALYZE = array_typanalyze,
	ALIGNMENT = 8,
	STORAGE = any,
	CATEGORY = A,
	ELEMENT = banking.account_type,
	DELIMITER = ',');

-- DROP TYPE banking."_role";

CREATE TYPE banking."_role" (
	INPUT = array_in,
	OUTPUT = array_out,
	RECEIVE = array_recv,
	SEND = array_send,
	ANALYZE = array_typanalyze,
	ALIGNMENT = 8,
	STORAGE = any,
	CATEGORY = A,
	ELEMENT = banking."role",
	DELIMITER = ',');

-- DROP TYPE banking."_users";

CREATE TYPE banking."_users" (
	INPUT = array_in,
	OUTPUT = array_out,
	RECEIVE = array_recv,
	SEND = array_send,
	ANALYZE = array_typanalyze,
	ALIGNMENT = 8,
	STORAGE = any,
	CATEGORY = A,
	ELEMENT = banking.users,
	DELIMITER = ',');

-- DROP TYPE banking.account;

CREATE TYPE banking.account AS (
	account_id int4,
	balance numeric(2,0),
	account_status_id int4,
	account_type_id int4);

-- DROP TYPE banking.account_status;

CREATE TYPE banking.account_status AS (
	account_status_id int4,
	account_status varchar);

-- DROP TYPE banking.account_type;

CREATE TYPE banking.account_type AS (
	account_type_id int4,
	account_type varchar);

-- DROP TYPE banking."role";

CREATE TYPE banking."role" AS (
	role_id int4,
	"role" varchar);

-- DROP TYPE banking.users;

CREATE TYPE banking.users AS (
	user_id int4,
	username varchar,
	"password" varchar,
	first_name varchar,
	last_name varchar,
	email varchar,
	role_id int4);
-- banking.account_status definition

-- Drop table

-- DROP TABLE banking.account_status;

CREATE TABLE banking.account_status (
	account_status_id int4 NOT NULL,
	account_status varchar NOT NULL,
	CONSTRAINT account_status_pk PRIMARY KEY (account_status_id)
);


-- banking.account_type definition

-- Drop table

-- DROP TABLE banking.account_type;

CREATE TABLE banking.account_type (
	account_type_id int4 NOT NULL,
	account_type varchar NOT NULL,
	CONSTRAINT account_type_pk PRIMARY KEY (account_type_id)
);


-- banking."role" definition

-- Drop table

-- DROP TABLE banking."role";

CREATE TABLE banking."role" (
	role_id int4 NOT NULL,
	"role" varchar NOT NULL,
	CONSTRAINT role_pk PRIMARY KEY (role_id)
);


-- banking.account definition

-- Drop table

-- DROP TABLE banking.account;

CREATE TABLE banking.account (
	account_id int4 NOT NULL,
	balance numeric(2) NOT NULL,
	account_status_id int4 NOT NULL,
	account_type_id int4 NOT NULL,
	CONSTRAINT account_pk PRIMARY KEY (account_id),
	CONSTRAINT account_fk FOREIGN KEY (account_status_id) REFERENCES banking.account_status(account_status_id),
	CONSTRAINT account_fk_1 FOREIGN KEY (account_type_id) REFERENCES banking.account_type(account_type_id)
);


-- banking.users definition

-- Drop table

-- DROP TABLE banking.users;

CREATE TABLE banking.users (
	user_id int4 NOT NULL,
	username varchar NOT NULL,
	"password" varchar NOT NULL,
	first_name varchar NOT NULL,
	last_name varchar NOT NULL,
	email varchar NOT NULL,
	role_id int4 NULL,
	CONSTRAINT users_pk PRIMARY KEY (user_id),
	CONSTRAINT users_un UNIQUE (username),
	CONSTRAINT users_fk FOREIGN KEY (role_id) REFERENCES banking.role(role_id)
);


-- banking.account_users definition

-- Drop table

-- DROP TABLE banking.account_users;

CREATE TABLE banking.account_users (
	account_id int4 NOT NULL,
	user_id int4 NOT NULL,
	roll_id int4 NOT NULL,
	account_users_id int4 NOT NULL,
	CONSTRAINT account_users_pk PRIMARY KEY (account_users_id),
	CONSTRAINT account_users_fk FOREIGN KEY (account_id) REFERENCES banking.account(account_id),
	CONSTRAINT account_users_fk_1 FOREIGN KEY (user_id) REFERENCES banking.users(user_id),
	CONSTRAINT account_users_fk_2 FOREIGN KEY (roll_id) REFERENCES banking.role(role_id)
);


