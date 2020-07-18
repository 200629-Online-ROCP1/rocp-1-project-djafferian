DROP SCHEMA banking CASCADE;

CREATE SCHEMA banking AUTHORIZATION postgres;

COMMENT ON SCHEMA banking IS '200629-Online-ROCP1';

-- DROP TYPE banking.roles;

CREATE TYPE banking.roles AS ENUM (
	'standard',
	'premium',
	'employee',
	'administrator');

-- DROP TYPE banking.statuses;

CREATE TYPE banking.statuses AS ENUM (
	'pending',
	'open',
	'closed',
	'denied');

-- DROP TYPE banking."types";

CREATE TYPE banking."types" AS ENUM (
	'checking',
	'savings');

-- DROP SEQUENCE banking.account_account_id_seq;

CREATE SEQUENCE banking.account_account_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE banking.users_user_id_seq;

CREATE SEQUENCE banking.users_user_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;-- banking.account definition

-- Drop table

-- DROP TABLE banking.account;

CREATE TABLE banking.account (
	account_id serial NOT NULL,
	balance numeric(2) NOT NULL,
	status banking.statuses NULL,
	"type" banking."types" NULL,
	CONSTRAINT account_pk PRIMARY KEY (account_id)
);


-- banking.users definition

-- Drop table

-- DROP TABLE banking.users;

CREATE TABLE banking.users (
	user_id serial NOT NULL,
	username varchar NOT NULL,
	"password" varchar NOT NULL,
	first_name varchar NOT NULL,
	last_name varchar NOT NULL,
	email varchar NOT NULL,
	"role" banking.roles NULL,
	CONSTRAINT users_pk PRIMARY KEY (user_id),
	CONSTRAINT users_un UNIQUE (username)
);


-- banking.account_users definition

-- Drop table

-- DROP TABLE banking.account_users;

CREATE TABLE banking.account_users (
	account_id int4 NOT NULL,
	user_id int4 NOT NULL,
	CONSTRAINT account_users_fk FOREIGN KEY (account_id) REFERENCES account(account_id),
	CONSTRAINT account_users_fk_1 FOREIGN KEY (user_id) REFERENCES users(user_id)
);

