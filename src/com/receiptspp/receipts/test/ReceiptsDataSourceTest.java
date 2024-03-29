package com.receiptspp.receipts.test;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.test.AndroidTestCase;

import com.receiptspp.business.DateHelper;
import com.receiptspp.business.Item;
import com.receiptspp.business.Receipt;
import com.receiptspp.business.UserData;
import com.receiptspp.database.ReceiptsDataSource;
import com.receiptspp.database.ReceiptsppContract.ItemsC;
import com.receiptspp.database.ReceiptsppContract.ItemsReceiptsMapC;
import com.receiptspp.database.ReceiptsppContract.ReceiptsC;
import com.receiptspp.database.ReceiptsppDbHelper;

public class ReceiptsDataSourceTest extends AndroidTestCase {

	private static final String EXAMPLE_STORE_1 = "Coffee General";
	private static final String EXAMPLE_CATEGORY_1 = "Dining";
	private static final double EXAMPLE_TOTAL_1 = 10.0;
	private static final int EXAMPLE_SERVER_ID_1 = 1;

	private ReceiptsDataSource mRds;
	private ReceiptsppDbHelper mDbHelper;
	private Receipt mReceipt;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mRds = new ReceiptsDataSource(getContext());
		mDbHelper = new ReceiptsppDbHelper(getContext());
		String date = new DateHelper().getFullDateTime();
		mReceipt = new Receipt(EXAMPLE_STORE_1, EXAMPLE_CATEGORY_1, date,
				EXAMPLE_SERVER_ID_1, EXAMPLE_TOTAL_1);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		mRds.deleteAllReceitps();
	}

	@Override
	public void testAndroidTestCaseSetupProperly() {
		super.testAndroidTestCaseSetupProperly();
	}

	public void testDeleteAllReceitps() {
		// create a receipt.
		Receipt receipt = new Receipt(EXAMPLE_STORE_1, EXAMPLE_CATEGORY_1,
				new DateHelper().getFullDateTime(), EXAMPLE_SERVER_ID_1, 0.0);
		// give it some items.
		List<Item> items = new ArrayList<Item>();
		items.add(new Item("Latte", EXAMPLE_CATEGORY_1, 2, 3.5));
		items.add(new Item("Beans", EXAMPLE_CATEGORY_1, 1, 15.0));
		for (Item i : items) {
			receipt.addItem(i);
		}
		Long newReceiptRow = mRds.createReceipt(receipt);
		assertEquals(1, getTableSize(ReceiptsC.TABLE_NAME));
		assertEquals(items.size(), getTableSize(ItemsC.TABLE_NAME));
		assertEquals(items.size(), getTableSize(ItemsReceiptsMapC.TABLE_NAME));

		mRds.deleteAllReceitps();
		assertEquals(0, getTableSize(ReceiptsC.TABLE_NAME));
		assertEquals(0, getTableSize(ItemsC.TABLE_NAME));
		assertEquals(0, getTableSize(ItemsReceiptsMapC.TABLE_NAME));
	}

	public void testCreateReceipt() {
		Long result = mRds.createReceipt(mReceipt);
		Long neg1 = (long) -1;
		assertNotSame(neg1, result);
		assertEquals(1, getTableSize(ReceiptsC.TABLE_NAME));
	}

	public void testReadReceipt() {
		Long result = mRds.createReceipt(mReceipt);
		if (result != -1) {
			Receipt readFromDb = mRds.readReceipt(EXAMPLE_SERVER_ID_1);
			assertEquals(mReceipt, readFromDb);
		} else {
			fail();
		}
	}

	public void testReadAllReceipts() {
		Long result = mRds.createReceipt(mReceipt);
		if (result != -1) {
			List<Receipt> receipts = mRds.readAllReceipts();
			assertEquals(1, receipts.size());
			assertEquals(mReceipt, receipts.get(0));
		} else {
			fail();
		}
	}

	public void testCreateItemWithServerId() {
		// first create a receipt
		Long newReceiptRow = mRds.createReceipt(mReceipt);
		if (newReceiptRow != -1) {
			// create an item for that receipt with the correct server id.
			Item item = new Item("Flat White", EXAMPLE_CATEGORY_1, 2, 5.0);
			long newItemRow = mRds.createItem(item, EXAMPLE_SERVER_ID_1);
			// check all the tables have 1 row.
			assertEquals(1, getTableSize(ReceiptsC.TABLE_NAME));
			assertEquals(1, getTableSize(ItemsC.TABLE_NAME));
			assertEquals(1, getTableSize(ItemsReceiptsMapC.TABLE_NAME));
			// get the mapping from the map table for this item row.
			List<Long> itemIds = getItemIdsForReceiptId(newReceiptRow);
			assertEquals(1, itemIds.size());
			assertEquals(newItemRow, (long) itemIds.get(0));
		} else {
			fail();
		}
	}

	public void testForeignKey() {
		// we will try to make a failing insert into the map.
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		// database is empty so if we reference the 1 id in both receipts and
		// items table the db should break
		ContentValues values = new ContentValues();
		values.put(ItemsReceiptsMapC.COLUMN_NAME_ITEMS_ID, 123);
		values.put(ItemsReceiptsMapC.COLUMN_NAME_RECEIPTS_ID, 123);
		try {
			db.insertOrThrow(ItemsReceiptsMapC.TABLE_NAME, null, values);
			// should throw
			fail();
		} catch (SQLiteException e) {
			// exception was thrown there pass/return
			return;
		}
	}

	public void testGetAllItemsInReceipt() {
		// create a receipt add it.
		Receipt receipt = new Receipt(EXAMPLE_STORE_1, EXAMPLE_CATEGORY_1,
				new DateHelper().getFullDateTime(), EXAMPLE_SERVER_ID_1, 0.0);
		// add some items.
		List<Item> items = new ArrayList<Item>();
		items.add(new Item("Latte", EXAMPLE_CATEGORY_1, 2, 3.5));
		items.add(new Item("Beans", EXAMPLE_CATEGORY_1, 1, 15.0));

		Long newReceiptRow = mRds.createReceipt(receipt);
		if (newReceiptRow != -1) {
			// create an item for that receipt with the correct server id.
			List<Long> createdItemIds = new ArrayList<Long>();
			createdItemIds.add(mRds.createItem(items.get(0),
					EXAMPLE_SERVER_ID_1));
			createdItemIds.add(mRds.createItem(items.get(1),
					EXAMPLE_SERVER_ID_1));
			assertEquals(1, getTableSize(ReceiptsC.TABLE_NAME));
			assertEquals(items.size(), getTableSize(ItemsC.TABLE_NAME));
			assertEquals(items.size(),
					getTableSize(ItemsReceiptsMapC.TABLE_NAME));
			// get the mapping from the map table for this item row.
			List<Long> retrievedItemIds = getItemIdsForReceiptId(newReceiptRow);
			assertEquals(items.size(), retrievedItemIds.size());
			// all the createdItemIds should also be in the itemIds
			for (Long l : createdItemIds) {
				if (!retrievedItemIds.contains(l)) {
					fail();
				}
			}
		} else {
			fail();
		}
	}

	public void testAddReceiptContainingItems() {
		// create a receipt.
		Receipt receipt = new Receipt(EXAMPLE_STORE_1, EXAMPLE_CATEGORY_1,
				new DateHelper().getFullDateTime(), EXAMPLE_SERVER_ID_1, 0.0);
		// give it some items.
		List<Item> items = new ArrayList<Item>();
		items.add(new Item("Latte", EXAMPLE_CATEGORY_1, 2, 3.5));
		items.add(new Item("Beans", EXAMPLE_CATEGORY_1, 1, 15.0));
		for (Item i : items) {
			receipt.addItem(i);
		}

		Long newReceiptRow = mRds.createReceipt(receipt);
		if (newReceiptRow != -1) {
			assertEquals(1, getTableSize(ReceiptsC.TABLE_NAME));
			assertEquals(items.size(), getTableSize(ItemsC.TABLE_NAME));
			assertEquals(items.size(),
					getTableSize(ItemsReceiptsMapC.TABLE_NAME));
			List<Item> retrievedItems = mRds
					.readItemsForServerId(EXAMPLE_SERVER_ID_1);
			assertEquals(items.size(), retrievedItems.size());
			for (Item retrievedItem : retrievedItems) {
				if (!items.contains(retrievedItem)) {
					fail();
				}
			}
		} else {
			fail();
		}
	}

	public void testInsertionOfDuplicateReceiptServerIdsFails() {
		Long newReceiptRow1 = mRds.createReceipt(mReceipt);
		// check we added the first receipt
		assertTrue(newReceiptRow1 != -1);
		Long newReceiptRow2 = mRds.createReceipt(mReceipt);
		// check the second add failed
		assertTrue(newReceiptRow2 == -1);
	}

	public void testUserInstantGreatestServerReceiptIdSet() {
		UserData userInstance = UserData.getInstance();
		int oldId = userInstance.getGreatestServerReceiptId();
		// new instance so id should be 0
		assertEquals(0, oldId);
		// create receipt
		Long newReceiptRow1 = mRds.createReceipt(mReceipt);
		if (newReceiptRow1 == -1) {
			fail();
		} else {
			assertTrue(userInstance.getGreatestServerReceiptId() > oldId);
		}
	}

	public void testUserInstantGreatestServerReceiptIdUpdate() {
		UserData userInstance = UserData.getInstance();
		int oldId = userInstance.getGreatestServerReceiptId();
		// new instance so id should be 0
		assertEquals(0, oldId);
		// attempt to set to 0, should still be 0
		userInstance.setGreatestServerReceiptId(0);
		assertEquals(0, userInstance.getGreatestServerReceiptId());
		userInstance.setGreatestServerReceiptId(1);
		assertEquals(1, userInstance.getGreatestServerReceiptId());
	}

	public void testUserInstantGreatestServerReceiptIdReset() {
		UserData userInstance = UserData.getInstance();
		int oldId = userInstance.getGreatestServerReceiptId();
		// new instance so id should be 0
		assertEquals(0, oldId);
		// attempt to set to 0, should still be 0
		userInstance.setGreatestServerReceiptId(1);
		assertEquals(1, userInstance.getGreatestServerReceiptId());
		userInstance.resetGreatestServerReceiptId();
		assertEquals(0, userInstance.getGreatestServerReceiptId());
	}

	public void testGetLastDate() {
		// create a receipt.
		//yyyy-MM-dd HH:mm:ss
		List<Receipt> receipts = new ArrayList<Receipt>();
		receipts.add(new Receipt("Cafe", "Dining","2012-12-01 00:00:00", 1, 0.0));
		receipts.add(new Receipt("McDonalds", "Dining","2013-01-11 00:00:00", 2, 0.0));
		receipts.add(new Receipt("Amazon", "Clothes","2013-01-12 00:00:00", 3, 0.0));
		receipts.add(new Receipt("Pb Tech", "Electronics","2013-01-13 00:00:00", 4, 0.0));
		
		for(Receipt r : receipts){
			mRds.createReceipt(r);
		}
		
		DateHelper date = mRds.readLastDate();
		assertEquals("2013", date.getYear());
		assertEquals("01", date.getMonth());
		assertEquals("13", date.getDay());
	}

	private int getTableSize(String tableName) {
		SQLiteDatabase db = mDbHelper.getReadableDatabase(); 
		int tableCols = -1;

		String sql = "SELECT * FROM " + tableName;
		Cursor cursor = db.rawQuery(sql, null);

		tableCols = cursor.getCount();
		cursor.close();

		return tableCols;
	}

	private List<Long> getItemIdsForReceiptId(Long receiptId) {
		List<Long> ids = new ArrayList<Long>();
		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		String[] projection = { ItemsReceiptsMapC.COLUMN_NAME_ITEMS_ID };
		String selection = ItemsReceiptsMapC.COLUMN_NAME_RECEIPTS_ID + " = ?";
		String[] selectionArgs = { receiptId.toString() };
		Cursor cursor = db.query(ItemsReceiptsMapC.TABLE_NAME, projection,
				selection, selectionArgs, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			ids.add(cursor.getLong(0));
			cursor.moveToNext();
		}
		cursor.close();
		return ids;
	}
}
