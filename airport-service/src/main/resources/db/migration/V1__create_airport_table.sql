CREATE TABLE airports (
    iata_code VARCHAR(3) NOT NULL,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    number_of_runways INTEGER NOT NULL,
    utc_offset_minutes INTEGER NOT NULL,
    CONSTRAINT pk_airports PRIMARY KEY (iata_code),
    CONSTRAINT ck_airports_iata_code CHECK (
        CHAR_LENGTH(iata_code) = 3
        AND SUBSTRING(iata_code FROM 1 FOR 1) BETWEEN 'A' AND 'Z'
        AND SUBSTRING(iata_code FROM 2 FOR 1) BETWEEN 'A' AND 'Z'
        AND SUBSTRING(iata_code FROM 3 FOR 1) BETWEEN 'A' AND 'Z'
    ),
    CONSTRAINT ck_airports_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_airports_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_airports_runways CHECK (number_of_runways > 0),
    CONSTRAINT ck_airports_utc_offset CHECK (utc_offset_minutes BETWEEN -720 AND 840)
);
