insert into user_info(id,name,phone) values(10086,'张三','13800138000') on duplicate key update name=values(name);
insert into product(id,name,category,brand,price,tags,description,after_sales_policy) values
(1,'AirLite 14 学生轻薄本','Laptop','Aster',4999,'学生,轻薄本,5000','1.25kg轻薄机身，适合上课、论文和通勤。','未发货超过48小时可创建售后工单并优先催发。'),
(2,'CoreBook 15 性价比笔记本','Laptop','Core',4599,'学生,办公,预算','大屏办公和网课体验好，预算友好。','7天无理由，延迟发货可申请补偿。'),
(3,'ProDesk 27 显示器','Monitor','ViewX',1299,'显示器,办公','护眼高刷显示器。','签收后7天无理由。'),
(4,'SwiftPad 平板','Tablet','Nova',2999,'平板,学习','便携学习平板。','质量问题15天换新。'),
(5,'LiteMouse 无线鼠标','Accessory','Mobi',99,'鼠标,无线','轻便无线鼠标。','一年质保。') on duplicate key update name=values(name);
insert into order_info(id,user_id,product_id,status,created_at,paid_at,shipped_at,cancel_reason) values
(10001,10086,1,'PAID',date_sub(now(), interval 70 hour),date_sub(now(), interval 69 hour),null,null),
(10002,10086,1,'PAID',date_sub(now(), interval 55 hour),date_sub(now(), interval 55 hour),null,null),
(10003,10086,2,'SHIPPED',date_sub(now(), interval 80 hour),date_sub(now(), interval 79 hour),date_sub(now(), interval 60 hour),null)
on duplicate key update status=values(status), created_at=values(created_at), paid_at=values(paid_at), shipped_at=values(shipped_at), cancel_reason=values(cancel_reason);
