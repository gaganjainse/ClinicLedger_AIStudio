package com.villageclinicledger.data.local

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery

class VillageClinicLedgerProvider : ContentProvider() {

    private companion object {
        const val AUTHORITY = "com.villageclinicledger.provider"

        private const val PATIENTS = 100
        private const val PATIENTS_ID = 101
        private const val VILLAGES = 200
        private const val VILLAGES_ID = 201
        private const val ALIASES = 300
        private const val ALIASES_ID = 301
        private const val TRANSACTIONS = 400
        private const val TRANSACTIONS_ID = 401

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "patients", PATIENTS)
            addURI(AUTHORITY, "patients/#", PATIENTS_ID)
            addURI(AUTHORITY, "villages", VILLAGES)
            addURI(AUTHORITY, "villages/#", VILLAGES_ID)
            addURI(AUTHORITY, "aliases", ALIASES)
            addURI(AUTHORITY, "aliases/#", ALIASES_ID)
            addURI(AUTHORITY, "transactions", TRANSACTIONS)
            addURI(AUTHORITY, "transactions/#", TRANSACTIONS_ID)
        }

        private val TABLE_COLUMNS = mapOf(
            "patients" to arrayOf("id", "name", "village_id", "phone", "current_balance", "created_at", "updated_at"),
            "villages" to arrayOf("id", "name", "created_at"),
            "aliases" to arrayOf("id", "patient_id", "alias", "created_at"),
            "transactions" to arrayOf("id", "patient_id", "type", "amount", "notes", "created_at", "updated_at")
        )
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val db = getDatabase().openHelper.readableDatabase
        val match = uriMatcher.match(uri)
        val (table, id) = parseMatch(match, uri)

        val finalSelection = buildSelection(id, selection)
        val finalArgs = buildArgs(id, selectionArgs)

        val columns = TABLE_COLUMNS[table]?.joinToString(", ") ?: "*"
        val sql = buildString {
            append("SELECT $columns FROM $table")
            if (finalSelection != null) append(" WHERE $finalSelection")
            if (sortOrder != null) append(" ORDER BY $sortOrder")
        }
        val cursor = db.query(SimpleSQLiteQuery(sql.toString(), finalArgs ?: emptyArray()))
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        val isDir = when (uriMatcher.match(uri)) {
            PATIENTS, VILLAGES, ALIASES, TRANSACTIONS -> true
            PATIENTS_ID, VILLAGES_ID, ALIASES_ID, TRANSACTIONS_ID -> false
            else -> return null
        }
        val table = tableForUri(uri)
        val base = if (isDir) "vnd.android.cursor.dir" else "vnd.android.cursor.item"
        return "$base/vnd.com.villageclinicledger.$table"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (values == null) return null
        val db = getDatabase().openHelper.writableDatabase
        val table = tableForUri(uri)

        val id = db.insert(table, SQLiteDatabase.CONFLICT_REPLACE, values)
        if (id == -1L) return null

        val resultUri = ContentUris.withAppendedId(uri, id)
        context!!.contentResolver.notifyChange(resultUri, null)
        return resultUri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = getDatabase().openHelper.writableDatabase
        val match = uriMatcher.match(uri)
        val (table, id) = parseMatch(match, uri)

        val finalSelection = buildSelection(id, selection)
        val finalArgs = buildArgs(id, selectionArgs)

        val deleted = db.delete(table, finalSelection, finalArgs)
        if (deleted > 0) context!!.contentResolver.notifyChange(uri, null)
        return deleted
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        if (values == null) return 0
        val db = getDatabase().openHelper.writableDatabase
        val match = uriMatcher.match(uri)
        val (table, id) = parseMatch(match, uri)

        val finalSelection = buildSelection(id, selection)
        val finalArgs = buildArgs(id, selectionArgs)

        val updated = db.update(table, SQLiteDatabase.CONFLICT_REPLACE, values, finalSelection, finalArgs)
        if (updated > 0) context!!.contentResolver.notifyChange(uri, null)
        return updated
    }

    private fun getDatabase() = VillageClinicLedgerDatabase.getDatabase(context!!)

    private fun tableForUri(uri: Uri): String = when (uriMatcher.match(uri)) {
        PATIENTS, PATIENTS_ID -> "patients"
        VILLAGES, VILLAGES_ID -> "villages"
        ALIASES, ALIASES_ID -> "aliases"
        TRANSACTIONS, TRANSACTIONS_ID -> "transactions"
        else -> throw IllegalArgumentException("Unknown URI: $uri")
    }

    private fun parseMatch(match: Int, uri: Uri): Pair<String, Long?> {
        val table = tableForUri(uri)
        val id = when (match) {
            PATIENTS_ID, VILLAGES_ID, ALIASES_ID, TRANSACTIONS_ID -> ContentUris.parseId(uri)
            else -> null
        }
        return Pair(table, id)
    }

    private fun buildSelection(id: Long?, selection: String?): String? {
        if (id == null) return selection
        val idClause = "\"id\" = ?"
        return if (selection == null) idClause else "$idClause AND ($selection)"
    }

    private fun buildArgs(id: Long?, selectionArgs: Array<String>?): Array<String>? {
        if (id == null) return selectionArgs
        val idArg = id.toString()
        return if (selectionArgs == null) arrayOf(idArg) else arrayOf(idArg, *selectionArgs)
    }
}
