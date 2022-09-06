DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS stores;
DROP TABLE IF EXISTS cities;
DROP TABLE IF EXISTS states;


CREATE TABLE states (
  state_id   INTEGER PRIMARY KEY,
  state_name VARCHAR(30) NOT NULL
);


CREATE TABLE cities (
  city_id    INTEGER PRIMARY KEY,
  city_name  VARCHAR(30) NOT NULL,
  population INTEGER NOT NULL,
  state_id   INTEGER REFERENCES states (state_id)
);


CREATE TABLE stores (
  store_id       INTEGER PRIMARY KEY,
  city_id        INTEGER REFERENCES cities (city_id),
  property_costs INTEGER NOT NULL
);


CREATE TABLE employees (
  emp_id     INTEGER PRIMARY KEY,
  last_name  VARCHAR(30) NOT NULL,
  first_name VARCHAR(30) NOT NULL,

  home_loc_id INTEGER REFERENCES cities (city_id),
  work_loc_id INTEGER REFERENCES cities (city_id),

  salary     INTEGER NOT NULL,

  manager_id INTEGER
);
