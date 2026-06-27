create table if not exists user_info(id bigint primary key, name varchar(64), phone varchar(32));
create table if not exists product(id bigint primary key, name varchar(128), category varchar(64), brand varchar(64), price decimal(10,2), tags varchar(255), description varchar(512), after_sales_policy varchar(512));
create table if not exists order_info(id bigint primary key, user_id bigint, product_id bigint, status varchar(32), created_at datetime, paid_at datetime null, shipped_at datetime null, cancel_reason varchar(255));
create table if not exists ticket(id bigint primary key auto_increment, order_id bigint, user_id bigint, product_id bigint, reason varchar(255), status varchar(32), customer_reply text, created_at datetime);
create table if not exists agent_run(id bigint primary key auto_increment, user_input text, task_type varchar(64), status varchar(64), final_answer text, replay_from_run_id bigint null, created_at datetime, updated_at datetime);
create table if not exists agent_step(id bigint primary key auto_increment, run_id bigint, agent_name varchar(64), step_name varchar(128), result text, created_at datetime);
create table if not exists tool_call_log(id bigint primary key auto_increment, run_id bigint, tool_name varchar(128), arguments_json text, result_json mediumtext, elapsed_ms bigint, status varchar(32), error_message text, created_at datetime);
create table if not exists approval_request(id bigint primary key auto_increment, run_id bigint, action_name varchar(64), order_id bigint, reason varchar(255), status varchar(32), created_at datetime, handled_at datetime null);

create table if not exists model_call_log(id bigint primary key auto_increment, run_id bigint not null, scene varchar(128) not null, model_name varchar(128), elapsed_ms bigint not null, status varchar(32) not null, error_message text, created_at datetime not null, index idx_model_call_run_id(run_id));
