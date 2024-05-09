create table if not exists client_request
(
    id            varchar(64),
    created_by    varchar(50),
    created_date  date,
    created_time  time,
    modified_by   varchar(50),
    modified_date date,
    modified_time time,
    version       bigint,
    client_name   varchar(255),
    http_method   varchar(10),
    base_url      varchar(255),
    api_path      varchar(255),
    path_params   jsonb,
    query_params  jsonb,
    headers       jsonb,
    payload       jsonb,
    timeout_in_millis int default 0,
    primary key (id)
);

create table if not exists job_executor
(
    id            varchar(64),
    created_by    varchar(50),
    created_date  date,
    created_time  time,
    modified_by   varchar(50),
    modified_date date,
    modified_time time,
    version       bigint,
    client_id     varchar(64),
    job_name      varchar(255),
    job_group     varchar(50),
    job_data      jsonb,
    trigger_cron  varchar(255),
    active        boolean,
    primary key (id)
);

create table if not exists job_history
(
    id              varchar(64),
    created_by      varchar(50),
    created_date    date,
    created_time    time,
    modified_by     varchar(50),
    modified_date   date,
    modified_time   time,
    version         bigint,
    job_executor_id varchar(64),
    executed_date   date,
    executed_time   time,
    status          varchar(15),
    trigger_cron    varchar(255),
    primary key (id)
);

create table if not exists job_history_detail
(
    id              varchar(64),
    created_by      varchar(50),
    created_date    date,
    created_time    time,
    modified_by     varchar(50),
    modified_date   date,
    modified_time   time,
    version         bigint,
    job_history_id  varchar(64),
    job_executor_id varchar(64),
    client_request  jsonb,
    result_detail   jsonb,
    primary key (id)
);

create index if not exists idx_clientrequest_name on client_request(client_name);
create index if not exists idx_jobexecutor_name on job_executor(job_name);

insert into client_request
    (id, created_by, created_date, created_time, modified_by, modified_date, modified_time, version,
     client_name, http_method, base_url, api_path, path_params, query_params, headers, payload)
    values
    ('1', 'SYSTEM', now(), now(), 'SYSTEM', now(), now(), 0,
     'client-name', 'GET', 'https://8b3817ceae844514bd45aad137f8ee1d.api.mockbin.io', '/', null, null, '{"Content-Type":["application/json"]}', null);

insert into job_executor
(id, created_by, created_date, created_time, modified_by, modified_date, modified_time, version,
 client_id, job_name, job_group, job_data, trigger_cron, active)
values
('1', 'SYSTEM', now(), now(), 'SYSTEM', now(), now(), 0,
 '1', 'client-job', 'WebClientJob', '{
  "clientRequest": "{\"id\":\"1\",\"createdBy\":\"SYSTEM\",\"createdDate\":[2023,11,21],\"createdTime\":[10,50,0,390049000],\"modifiedBy\":\"SYSTEM\",\"modifiedDate\":[2023,11,21],\"modifiedTime\":[10,50,0,390049000],\"version\":1,\"clientName\":\"client-name\",\"httpMethod\":\"GET\",\"baseUrl\":\"https://8b3817ceae844514bd45aad137f8ee1d.api.mockbin.io\",\"apiPath\":\"/\",\"headers\":{\"Content-Type\":[\"application/json\"]},\"fullApiPath\":\"/\"}",
  "jobExecutorId": "1"
}', '0 0/5 0 ? * * *', false);