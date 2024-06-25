package io.mo;/*
 * jTPCCConnection
 *
 * One connection to the database. Used by either the old style
 * Terminal or the new TimedSUT.
 *
 * Copyright (C) 2004-2016, Denis Lussier
 * Copyright (C) 2016, Jan Wieck
 *
 */

import org.apache.log4j.Logger;

import java.sql.*;
import java.util.Properties;

public class jTPCCConnection
{
    private Connection          dbConn = null;
	
	private String 				jdbcURL = null;
	
	private Properties 			dbProps = null;
	
	private boolean				valid = true;

	private int                 dbType = 0;

    public PreparedStatement    stmtNewOrderSelectWhseCust;
    public PreparedStatement    stmtNewOrderSelectDist;
    public PreparedStatement    stmtNewOrderUpdateDist;
    public PreparedStatement    stmtNewOrderInsertOrder;
    public PreparedStatement    stmtNewOrderInsertNewOrder;
    public PreparedStatement    stmtNewOrderSelectStock;
    public PreparedStatement    stmtNewOrderSelectItem;
    public PreparedStatement    stmtNewOrderUpdateStock;
    public PreparedStatement    stmtNewOrderInsertOrderLine;

    public PreparedStatement    stmtPaymentSelectWarehouse;
    public PreparedStatement    stmtPaymentSelectDistrict;
    public PreparedStatement    stmtPaymentSelectCustomerListByLast;
    public PreparedStatement    stmtPaymentSelectCustomer;
    public PreparedStatement    stmtPaymentSelectCustomerData;
    public PreparedStatement    stmtPaymentUpdateWarehouse;
    public PreparedStatement    stmtPaymentUpdateDistrict;
    public PreparedStatement    stmtPaymentUpdateCustomer;
    public PreparedStatement    stmtPaymentUpdateCustomerWithData;
    public PreparedStatement    stmtPaymentInsertHistory;

    public PreparedStatement    stmtOrderStatusSelectCustomerListByLast;
    public PreparedStatement    stmtOrderStatusSelectCustomer;
    public PreparedStatement    stmtOrderStatusSelectLastOrder;
    public PreparedStatement    stmtOrderStatusSelectOrderLine;

    public PreparedStatement    stmtStockLevelSelectLow;

    public PreparedStatement    stmtDeliveryBGSelectOldestNewOrder;
    public PreparedStatement    stmtDeliveryBGDeleteOldestNewOrder;
    public PreparedStatement    stmtDeliveryBGSelectOrder;
    public PreparedStatement    stmtDeliveryBGUpdateOrder;
    public PreparedStatement    stmtDeliveryBGSelectSumOLAmount;
    public PreparedStatement    stmtDeliveryBGUpdateOrderLine;
    public PreparedStatement    stmtDeliveryBGUpdateCustomer;
	
	//test code
	public Statement stmtSumDistrict;

	private String conn_id = null;

	private static org.apache.log4j.Logger log = Logger.getLogger(jTPCCConnection.class);

    public jTPCCConnection(Connection dbConn, int dbType)
	throws SQLException
    {
	this.dbConn = dbConn;
	this.dbType = dbType;
	//test code
	stmtSumDistrict = dbConn.createStatement();

	// PreparedStataments for NEW_ORDER
	stmtNewOrderSelectWhseCust = dbConn.prepareStatement(
		"SELECT c_discount, c_last, c_credit, w_tax " +
		"    FROM bmsql_customer " +
		"    JOIN bmsql_warehouse ON (w_id = c_w_id) " +
		"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ?");
	
	stmtNewOrderSelectDist = dbConn.prepareStatement(
		"SELECT d_tax, d_next_o_id " +
		"    FROM bmsql_district " +
		"    WHERE d_w_id = ? AND d_id = ? " +
		"    FOR UPDATE");
	/*stmtNewOrderSelectDist = dbConn.prepareStatement(
		"SELECT d_tax, d_next_o_id " +
				"    FROM bmsql_district " +
				"    WHERE d_w_id = ? AND d_id = ? ");*/
	stmtNewOrderUpdateDist = dbConn.prepareStatement(
		"UPDATE bmsql_district " +
		"    SET d_next_o_id = d_next_o_id + 1 " +
		"    WHERE d_w_id = ? AND d_id = ?");
	stmtNewOrderInsertOrder = dbConn.prepareStatement(
		"INSERT INTO bmsql_oorder (" +
		"    o_id, o_d_id, o_w_id, o_c_id, o_entry_d, " +
		"    o_ol_cnt, o_all_local) " +
		"VALUES (?, ?, ?, ?, ?, ?, ?)");
	stmtNewOrderInsertNewOrder = dbConn.prepareStatement(
		"INSERT INTO bmsql_new_order (" +
		"    no_o_id, no_d_id, no_w_id) " +
		"VALUES (?, ?, ?)");
	stmtNewOrderSelectStock = dbConn.prepareStatement(
		"SELECT s_quantity, s_data, " +
		"       s_dist_01, s_dist_02, s_dist_03, s_dist_04, " +
		"       s_dist_05, s_dist_06, s_dist_07, s_dist_08, " +
		"       s_dist_09, s_dist_10 " +
		"    FROM bmsql_stock " +
		"    WHERE s_w_id = ? AND s_i_id = ? " +
		"    FOR UPDATE");
	/*stmtNewOrderSelectStock = dbConn.prepareStatement(
		"SELECT s_quantity, s_data, " +
				"       s_dist_01, s_dist_02, s_dist_03, s_dist_04, " +
				"       s_dist_05, s_dist_06, s_dist_07, s_dist_08, " +
				"       s_dist_09, s_dist_10 " +
				"    FROM bmsql_stock " +
				"    WHERE s_w_id = ? AND s_i_id = ? " );*/
	stmtNewOrderSelectItem = dbConn.prepareStatement(
		"SELECT i_price, i_name, i_data " +
		"    FROM bmsql_item " +
		"    WHERE i_id = ?");
	stmtNewOrderUpdateStock = dbConn.prepareStatement(
		"UPDATE bmsql_stock " +
		"    SET s_quantity = ?, s_ytd = s_ytd + ?, " +
		"        s_order_cnt = s_order_cnt + 1, " +
		"        s_remote_cnt = s_remote_cnt + ? " +
		"    WHERE s_w_id = ? AND s_i_id = ?");
	stmtNewOrderInsertOrderLine = dbConn.prepareStatement(
		"INSERT INTO bmsql_order_line (" +
		"    ol_o_id, ol_d_id, ol_w_id, ol_number, " +
		"    ol_i_id, ol_supply_w_id, ol_quantity, " +
		"    ol_amount, ol_dist_info) " +
		"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

	// PreparedStatements for PAYMENT
	stmtPaymentSelectWarehouse = dbConn.prepareStatement(
		"SELECT w_name, w_street_1, w_street_2, w_city, " +
		"       w_state, w_zip, w_ytd " +
		"    FROM bmsql_warehouse " +
		"    WHERE w_id = ? ");
	stmtPaymentSelectDistrict = dbConn.prepareStatement(
		"SELECT d_name, d_street_1, d_street_2, d_city, " +
		"       d_state, d_zip, d_ytd " +
		"    FROM bmsql_district " +
		"    WHERE d_w_id = ? AND d_id = ?");
	stmtPaymentSelectCustomerListByLast = dbConn.prepareStatement(
		"SELECT c_id " +
		"    FROM bmsql_customer " +
		"    WHERE c_w_id = ? AND c_d_id = ? AND c_last = ? " +
		"    ORDER BY c_first");
	stmtPaymentSelectCustomer = dbConn.prepareStatement(
		"SELECT c_first, c_middle, c_last, c_street_1, c_street_2, " +
		"       c_city, c_state, c_zip, c_phone, c_since, c_credit, " +
		"       c_credit_lim, c_discount, c_balance " +
		"    FROM bmsql_customer " +
		"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ? " +
		"    FOR UPDATE");
	/*stmtPaymentSelectCustomer = dbConn.prepareStatement(
		"SELECT c_first, c_middle, c_last, c_street_1, c_street_2, " +
				"       c_city, c_state, c_zip, c_phone, c_since, c_credit, " +
				"       c_credit_lim, c_discount, c_balance " +
				"    FROM bmsql_customer " +
				"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ? ");*/
	stmtPaymentSelectCustomerData = dbConn.prepareStatement(
		"SELECT c_data " +
		"    FROM bmsql_customer " +
		"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ?");
	stmtPaymentUpdateWarehouse = dbConn.prepareStatement(
		"UPDATE bmsql_warehouse " +
		"    SET w_ytd = w_ytd + ? " +
		"    WHERE w_id = ?");
	stmtPaymentUpdateDistrict = dbConn.prepareStatement(
		"UPDATE bmsql_district " +
		"    SET d_ytd = d_ytd + ? " +
		"    WHERE d_w_id = ? AND d_id = ?");
	stmtPaymentUpdateCustomer = dbConn.prepareStatement(
		"UPDATE bmsql_customer " +
		"    SET c_balance = c_balance - ?, " +
		"        c_ytd_payment = c_ytd_payment + ?, " +
		"        c_payment_cnt = c_payment_cnt + 1 " +
		"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ?");
	stmtPaymentUpdateCustomerWithData = dbConn.prepareStatement(
		"UPDATE bmsql_customer " +
		"    SET c_balance = c_balance - ?, " +
		"        c_ytd_payment = c_ytd_payment + ?, " +
		"        c_payment_cnt = c_payment_cnt + 1, " +
		"        c_data = ? " +
		"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ?");
	stmtPaymentInsertHistory = dbConn.prepareStatement(
		"INSERT INTO bmsql_history (" +
		"    h_c_id, h_c_d_id, h_c_w_id, h_d_id, h_w_id, " +
		"    h_date, h_amount, h_data) " +
		"VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

	// PreparedStatements for ORDER_STATUS
	stmtOrderStatusSelectCustomerListByLast = dbConn.prepareStatement(
		"SELECT c_id " +
		"    FROM bmsql_customer " +
		"    WHERE c_w_id = ? AND c_d_id = ? AND c_last = ? " +
		"    ORDER BY c_first");
	stmtOrderStatusSelectCustomer = dbConn.prepareStatement(
		"SELECT c_first, c_middle, c_last, c_balance " +
		"    FROM bmsql_customer " +
		"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ?");
	stmtOrderStatusSelectLastOrder = dbConn.prepareStatement(
		"SELECT o_id, o_entry_d, o_carrier_id " +
		"    FROM bmsql_oorder " +
		"    WHERE o_w_id = ? AND o_d_id = ? AND o_c_id = ? " +
		"      AND o_id = (" +
		"          SELECT max(o_id) " +
		"              FROM bmsql_oorder " +
		"              WHERE o_w_id = ? AND o_d_id = ? AND o_c_id = ?" +
		"          )");
	stmtOrderStatusSelectOrderLine = dbConn.prepareStatement(
		"SELECT ol_i_id, ol_supply_w_id, ol_quantity, " +
		"       ol_amount, ol_delivery_d " +
		"    FROM bmsql_order_line " +
		"    WHERE ol_w_id = ? AND ol_d_id = ? AND ol_o_id = ? " +
		"    ORDER BY ol_w_id, ol_d_id, ol_o_id, ol_number");

	// PreparedStatements for STOCK_LEVEL
	switch (dbType)
	{
	    case jTPCCConfig.DB_POSTGRES:
		stmtStockLevelSelectLow = dbConn.prepareStatement(
		    "SELECT count(*) AS low_stock FROM (" +
		    "    SELECT s_w_id, s_i_id, s_quantity " +
		    "        FROM bmsql_stock " +
		    "        WHERE s_w_id = ? AND s_quantity < ? AND s_i_id IN (" +
		    "            SELECT ol_i_id " +
		    "                FROM bmsql_district " +
		    "                JOIN bmsql_order_line ON ol_w_id = d_w_id " +
		    "                 AND ol_d_id = d_id " +
		    "                 AND ol_o_id >= d_next_o_id - 20 " +
		    "                 AND ol_o_id < d_next_o_id " +
		    "                WHERE d_w_id = ? AND d_id = ? " +
		    "        ) " +
		    "    ) AS L");
		break;

	    default:
		stmtStockLevelSelectLow = dbConn.prepareStatement(
		    "SELECT count(*) AS low_stock FROM (" +
		    "    SELECT s_w_id, s_i_id, s_quantity " +
		    "        FROM bmsql_stock " +
		    "        WHERE s_w_id = ? AND s_quantity < ? AND s_i_id IN (" +
		    "            SELECT ol_i_id " +
		    "                FROM bmsql_district " +
		    "                JOIN bmsql_order_line ON ol_w_id = d_w_id " +
		    "                 AND ol_d_id = d_id " +
		    "                 AND ol_o_id >= d_next_o_id - 20 " +
		    "                 AND ol_o_id < d_next_o_id " +
		    "                WHERE d_w_id = ? AND d_id = ? " +
		    "        ) " +
		    "    ) AS L");
		break;
	}

	// PreparedStatements for DELIVERY_BG
	stmtDeliveryBGSelectOldestNewOrder = dbConn.prepareStatement(
		"SELECT no_o_id " +
		"    FROM bmsql_new_order " +
		"    WHERE no_w_id = ? AND no_d_id = ? " +
		"    ORDER BY no_o_id ASC");
	stmtDeliveryBGDeleteOldestNewOrder = dbConn.prepareStatement(
		"DELETE FROM bmsql_new_order " +
		"    WHERE no_w_id = ? AND no_d_id = ? AND no_o_id = ?");
	stmtDeliveryBGSelectOrder = dbConn.prepareStatement(
		"SELECT o_c_id " +
		"    FROM bmsql_oorder " +
		"    WHERE o_w_id = ? AND o_d_id = ? AND o_id = ?");
	stmtDeliveryBGUpdateOrder = dbConn.prepareStatement(
		"UPDATE bmsql_oorder " +
		"    SET o_carrier_id = ? " +
		"    WHERE o_w_id = ? AND o_d_id = ? AND o_id = ?");
	stmtDeliveryBGSelectSumOLAmount = dbConn.prepareStatement(
		"SELECT sum(ol_amount) AS sum_ol_amount " +
		"    FROM bmsql_order_line " +
		"    WHERE ol_w_id = ? AND ol_d_id = ? AND ol_o_id = ?");
	stmtDeliveryBGUpdateOrderLine = dbConn.prepareStatement(
		"UPDATE bmsql_order_line " +
		"    SET ol_delivery_d = ? " +
		"    WHERE ol_w_id = ? AND ol_d_id = ? AND ol_o_id = ?");
	stmtDeliveryBGUpdateCustomer = dbConn.prepareStatement(
		"UPDATE bmsql_customer " +
		"    SET c_balance = c_balance + ?, " +
		"        c_delivery_cnt = c_delivery_cnt + 1 " +
		"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ?");
    }

    public jTPCCConnection(String connURL, Properties connProps, int dbType)
	throws SQLException
    {
	this(DriverManager.getConnection(connURL, connProps), dbType);
    }

    public void commit()
	throws SQLException
    {
	dbConn.commit();
    }

    public void rollback()
	throws SQLException
    {
		dbConn.rollback();
    }
	
	public boolean isValid(){
		return this.valid;
	}
	
	public void checkStatus(){
		try {
			 if(this.dbConn.isClosed()) {
				 log.error(String.format("The connection[%s] has been closed.",conn_id));
				 this.valid = false;
			 }
			 
			 if(!this.dbConn.isValid(30)) {
				 log.error(String.format("The connection[%s] has not been valid.",conn_id));
				 this.valid = false;
			 }
			 
		} catch (SQLException e) {
			log.error("The connection has been lost, caused by: " + e.getMessage());
			this.valid = false;
		}
	}

	public boolean is(){
		try {
			return this.dbConn.isClosed();
		} catch (SQLException e) {
			return true;
		}
	}
	
	public void reConnect(){
		for(int i = 0; i < 3; i++) {
			try {
				if(!this.dbConn.isClosed())
					this.dbConn.close();
				this.dbConn = DriverManager.getConnection(this.jdbcURL,this.dbProps);
				String new_conn_id = getConnId(dbConn);
				System.out.println(String.format("The new connection[%s] has been established, and the old connection[%s] has been closed.",new_conn_id,this.conn_id));
				setConn_id(new_conn_id);
				prepareStmt();
				dbConn.setAutoCommit(false);
				this.valid = true;
				break;
			} catch (SQLException e) {
				try {
					Thread.sleep(60000);
				} catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
			}
		}
	}
	

	public void setJdbcURL(String URL) {
		this.jdbcURL = URL;
	}

	public void setDbProps(Properties dbProps) {
		this.dbProps = dbProps;
	}

	public void prepareStmt() throws SQLException{
		// PreparedStataments for NEW_ORDER
		stmtNewOrderSelectWhseCust = dbConn.prepareStatement(
				"SELECT c_discount, c_last, c_credit, w_tax " +
						"    FROM bmsql_customer " +
						"    JOIN bmsql_warehouse ON (w_id = c_w_id) " +
						"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ?");

		stmtNewOrderSelectDist = dbConn.prepareStatement(
				"SELECT d_tax, d_next_o_id " +
						"    FROM bmsql_district " +
						"    WHERE d_w_id = ? AND d_id = ? " +
						"    FOR UPDATE");
	/*stmtNewOrderSelectDist = dbConn.prepareStatement(
		"SELECT d_tax, d_next_o_id " +
				"    FROM bmsql_district " +
				"    WHERE d_w_id = ? AND d_id = ? ");*/
		stmtNewOrderUpdateDist = dbConn.prepareStatement(
				"UPDATE bmsql_district " +
						"    SET d_next_o_id = d_next_o_id + 1 " +
						"    WHERE d_w_id = ? AND d_id = ?");
		stmtNewOrderInsertOrder = dbConn.prepareStatement(
				"INSERT INTO bmsql_oorder (" +
						"    o_id, o_d_id, o_w_id, o_c_id, o_entry_d, " +
						"    o_ol_cnt, o_all_local) " +
						"VALUES (?, ?, ?, ?, ?, ?, ?)");
		stmtNewOrderInsertNewOrder = dbConn.prepareStatement(
				"INSERT INTO bmsql_new_order (" +
						"    no_o_id, no_d_id, no_w_id) " +
						"VALUES (?, ?, ?)");
		stmtNewOrderSelectStock = dbConn.prepareStatement(
				"SELECT s_quantity, s_data, " +
						"       s_dist_01, s_dist_02, s_dist_03, s_dist_04, " +
						"       s_dist_05, s_dist_06, s_dist_07, s_dist_08, " +
						"       s_dist_09, s_dist_10 " +
						"    FROM bmsql_stock " +
						"    WHERE s_w_id = ? AND s_i_id = ? " +
						"    FOR UPDATE");
	/*stmtNewOrderSelectStock = dbConn.prepareStatement(
		"SELECT s_quantity, s_data, " +
				"       s_dist_01, s_dist_02, s_dist_03, s_dist_04, " +
				"       s_dist_05, s_dist_06, s_dist_07, s_dist_08, " +
				"       s_dist_09, s_dist_10 " +
				"    FROM bmsql_stock " +
				"    WHERE s_w_id = ? AND s_i_id = ? " );*/
		stmtNewOrderSelectItem = dbConn.prepareStatement(
				"SELECT i_price, i_name, i_data " +
						"    FROM bmsql_item " +
						"    WHERE i_id = ?");
		stmtNewOrderUpdateStock = dbConn.prepareStatement(
				"UPDATE bmsql_stock " +
						"    SET s_quantity = ?, s_ytd = s_ytd + ?, " +
						"        s_order_cnt = s_order_cnt + 1, " +
						"        s_remote_cnt = s_remote_cnt + ? " +
						"    WHERE s_w_id = ? AND s_i_id = ?");
		stmtNewOrderInsertOrderLine = dbConn.prepareStatement(
				"INSERT INTO bmsql_order_line (" +
						"    ol_o_id, ol_d_id, ol_w_id, ol_number, " +
						"    ol_i_id, ol_supply_w_id, ol_quantity, " +
						"    ol_amount, ol_dist_info) " +
						"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

		// PreparedStatements for PAYMENT
		stmtPaymentSelectWarehouse = dbConn.prepareStatement(
				"SELECT w_name, w_street_1, w_street_2, w_city, " +
						"       w_state, w_zip, w_ytd " +
						"    FROM bmsql_warehouse " +
						"    WHERE w_id = ? ");
		stmtPaymentSelectDistrict = dbConn.prepareStatement(
				"SELECT d_name, d_street_1, d_street_2, d_city, " +
						"       d_state, d_zip, d_ytd " +
						"    FROM bmsql_district " +
						"    WHERE d_w_id = ? AND d_id = ?");
		stmtPaymentSelectCustomerListByLast = dbConn.prepareStatement(
				"SELECT c_id " +
						"    FROM bmsql_customer " +
						"    WHERE c_w_id = ? AND c_d_id = ? AND c_last = ? " +
						"    ORDER BY c_first");
		stmtPaymentSelectCustomer = dbConn.prepareStatement(
				"SELECT c_first, c_middle, c_last, c_street_1, c_street_2, " +
						"       c_city, c_state, c_zip, c_phone, c_since, c_credit, " +
						"       c_credit_lim, c_discount, c_balance " +
						"    FROM bmsql_customer " +
						"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ? " +
						"    FOR UPDATE");
	/*stmtPaymentSelectCustomer = dbConn.prepareStatement(
		"SELECT c_first, c_middle, c_last, c_street_1, c_street_2, " +
				"       c_city, c_state, c_zip, c_phone, c_since, c_credit, " +
				"       c_credit_lim, c_discount, c_balance " +
				"    FROM bmsql_customer " +
				"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ? ");*/
		stmtPaymentSelectCustomerData = dbConn.prepareStatement(
				"SELECT c_data " +
						"    FROM bmsql_customer " +
						"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ?");
		stmtPaymentUpdateWarehouse = dbConn.prepareStatement(
				"UPDATE bmsql_warehouse " +
						"    SET w_ytd = w_ytd + ? " +
						"    WHERE w_id = ?");
		stmtPaymentUpdateDistrict = dbConn.prepareStatement(
				"UPDATE bmsql_district " +
						"    SET d_ytd = d_ytd + ? " +
						"    WHERE d_w_id = ? AND d_id = ?");
		stmtPaymentUpdateCustomer = dbConn.prepareStatement(
				"UPDATE bmsql_customer " +
						"    SET c_balance = c_balance - ?, " +
						"        c_ytd_payment = c_ytd_payment + ?, " +
						"        c_payment_cnt = c_payment_cnt + 1 " +
						"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ?");
		stmtPaymentUpdateCustomerWithData = dbConn.prepareStatement(
				"UPDATE bmsql_customer " +
						"    SET c_balance = c_balance - ?, " +
						"        c_ytd_payment = c_ytd_payment + ?, " +
						"        c_payment_cnt = c_payment_cnt + 1, " +
						"        c_data = ? " +
						"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ?");
		stmtPaymentInsertHistory = dbConn.prepareStatement(
				"INSERT INTO bmsql_history (" +
						"    h_c_id, h_c_d_id, h_c_w_id, h_d_id, h_w_id, " +
						"    h_date, h_amount, h_data) " +
						"VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

		// PreparedStatements for ORDER_STATUS
		stmtOrderStatusSelectCustomerListByLast = dbConn.prepareStatement(
				"SELECT c_id " +
						"    FROM bmsql_customer " +
						"    WHERE c_w_id = ? AND c_d_id = ? AND c_last = ? " +
						"    ORDER BY c_first");
		stmtOrderStatusSelectCustomer = dbConn.prepareStatement(
				"SELECT c_first, c_middle, c_last, c_balance " +
						"    FROM bmsql_customer " +
						"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ?");
		stmtOrderStatusSelectLastOrder = dbConn.prepareStatement(
				"SELECT o_id, o_entry_d, o_carrier_id " +
						"    FROM bmsql_oorder " +
						"    WHERE o_w_id = ? AND o_d_id = ? AND o_c_id = ? " +
						"      AND o_id = (" +
						"          SELECT max(o_id) " +
						"              FROM bmsql_oorder " +
						"              WHERE o_w_id = ? AND o_d_id = ? AND o_c_id = ?" +
						"          )");
		stmtOrderStatusSelectOrderLine = dbConn.prepareStatement(
				"SELECT ol_i_id, ol_supply_w_id, ol_quantity, " +
						"       ol_amount, ol_delivery_d " +
						"    FROM bmsql_order_line " +
						"    WHERE ol_w_id = ? AND ol_d_id = ? AND ol_o_id = ? " +
						"    ORDER BY ol_w_id, ol_d_id, ol_o_id, ol_number");

		// PreparedStatements for STOCK_LEVEL
		switch (dbType)
		{
			case jTPCCConfig.DB_POSTGRES:
				stmtStockLevelSelectLow = dbConn.prepareStatement(
						"SELECT count(*) AS low_stock FROM (" +
								"    SELECT s_w_id, s_i_id, s_quantity " +
								"        FROM bmsql_stock " +
								"        WHERE s_w_id = ? AND s_quantity < ? AND s_i_id IN (" +
								"            SELECT ol_i_id " +
								"                FROM bmsql_district " +
								"                JOIN bmsql_order_line ON ol_w_id = d_w_id " +
								"                 AND ol_d_id = d_id " +
								"                 AND ol_o_id >= d_next_o_id - 20 " +
								"                 AND ol_o_id < d_next_o_id " +
								"                WHERE d_w_id = ? AND d_id = ? " +
								"        ) " +
								"    ) AS L");
				break;

			default:
				stmtStockLevelSelectLow = dbConn.prepareStatement(
						"SELECT count(*) AS low_stock FROM (" +
								"    SELECT s_w_id, s_i_id, s_quantity " +
								"        FROM bmsql_stock " +
								"        WHERE s_w_id = ? AND s_quantity < ? AND s_i_id IN (" +
								"            SELECT ol_i_id " +
								"                FROM bmsql_district " +
								"                JOIN bmsql_order_line ON ol_w_id = d_w_id " +
								"                 AND ol_d_id = d_id " +
								"                 AND ol_o_id >= d_next_o_id - 20 " +
								"                 AND ol_o_id < d_next_o_id " +
								"                WHERE d_w_id = ? AND d_id = ? " +
								"        ) " +
								"    ) AS L");
				break;
		}

		// PreparedStatements for DELIVERY_BG
		stmtDeliveryBGSelectOldestNewOrder = dbConn.prepareStatement(
				"SELECT no_o_id " +
						"    FROM bmsql_new_order " +
						"    WHERE no_w_id = ? AND no_d_id = ? " +
						"    ORDER BY no_o_id ASC");
		stmtDeliveryBGDeleteOldestNewOrder = dbConn.prepareStatement(
				"DELETE FROM bmsql_new_order " +
						"    WHERE no_w_id = ? AND no_d_id = ? AND no_o_id = ?");
		stmtDeliveryBGSelectOrder = dbConn.prepareStatement(
				"SELECT o_c_id " +
						"    FROM bmsql_oorder " +
						"    WHERE o_w_id = ? AND o_d_id = ? AND o_id = ?");
		stmtDeliveryBGUpdateOrder = dbConn.prepareStatement(
				"UPDATE bmsql_oorder " +
						"    SET o_carrier_id = ? " +
						"    WHERE o_w_id = ? AND o_d_id = ? AND o_id = ?");
		stmtDeliveryBGSelectSumOLAmount = dbConn.prepareStatement(
				"SELECT sum(ol_amount) AS sum_ol_amount " +
						"    FROM bmsql_order_line " +
						"    WHERE ol_w_id = ? AND ol_d_id = ? AND ol_o_id = ?");
		stmtDeliveryBGUpdateOrderLine = dbConn.prepareStatement(
				"UPDATE bmsql_order_line " +
						"    SET ol_delivery_d = ? " +
						"    WHERE ol_w_id = ? AND ol_d_id = ? AND ol_o_id = ?");
		stmtDeliveryBGUpdateCustomer = dbConn.prepareStatement(
				"UPDATE bmsql_customer " +
						"    SET c_balance = c_balance + ?, " +
						"        c_delivery_cnt = c_delivery_cnt + 1 " +
						"    WHERE c_w_id = ? AND c_d_id = ? AND c_id = ?");
	}

	public String getConn_id() {
		return conn_id;
	}

	public void setConn_id(String conn_id) {
		this.conn_id = conn_id;
	}

	public String getConnId(Connection connection){
		try {
			Statement queryConnId = connection.createStatement();
			ResultSet resultSet = queryConnId.executeQuery("select connection_id();");
			if(resultSet.next())
				return resultSet.getString(1);
			return null;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
}
