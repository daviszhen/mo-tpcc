load data infile '/Users/sudong/Program/aressu1985/mo-tpcc/data/config.csv' into table bmsql_config fields terminated by ',';
load data infile '/Users/sudong/Program/aressu1985/mo-tpcc/data/warehouse.csv' into table bmsql_warehouse fields terminated by ',';
load data infile '/Users/sudong/Program/aressu1985/mo-tpcc/data/district.csv' into table bmsql_district fields terminated by ',';
load data infile '/Users/sudong/Program/aressu1985/mo-tpcc/data/customer.csv' into table bmsql_customer fields terminated by ',';
load data infile '/Users/sudong/Program/aressu1985/mo-tpcc/data/cust-hist.csv' into table bmsql_history fields terminated by ',';
load data infile '/Users/sudong/Program/aressu1985/mo-tpcc/data/new-order.csv' into table bmsql_new_order fields terminated by ',';
load data infile '/Users/sudong/Program/aressu1985/mo-tpcc/data/order.csv' into table bmsql_oorder fields terminated by ',';
load data infile '/Users/sudong/Program/aressu1985/mo-tpcc/data/order-line.csv' into table bmsql_order_line fields terminated by ',';
load data infile '/Users/sudong/Program/aressu1985/mo-tpcc/data/item.csv' into table bmsql_item fields terminated by ',';
load data infile '/Users/sudong/Program/aressu1985/mo-tpcc/data/stock.csv' into table bmsql_stock fields terminated by ',';