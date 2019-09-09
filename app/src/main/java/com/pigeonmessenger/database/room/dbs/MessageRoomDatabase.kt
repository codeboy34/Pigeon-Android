package com.pigeonmessenger.database.room.dbs

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pigeonmessenger.database.room.daos.*
import com.pigeonmessenger.database.room.entities.AttachmentSessionEntity
import com.pigeonmessenger.database.room.entities.Conversation
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.utils.Constant
import com.pigeonmessenger.vo.*


@Database(entities = [MessageEntity::class,
    AttachmentSessionEntity::class,
    User::class, Conversation::class,
    FloodMessage::class, Hyperlink::class,
    Participant::class,
    SentSenderKey::class,
    ResendMessage::class],
        version = 76, exportSchema = false)
abstract class MessageRoomDatabase : RoomDatabase() {

    abstract fun getMessageDao(): MessageDao
    abstract fun getAttachmentSessionDao(): AttachmentSessionDao
    abstract fun getContactsDao(): ContactsDao
    abstract fun getConversationDao(): ConversationDao
    abstract fun getFloodMessageDao(): FloodMessageDao
    abstract fun hyperlinkDao(): HyperlinkDao
    abstract fun participantDao(): ParticipantDao
    abstract fun sentSenderKeyDao(): SentSenderKeyDao
    abstract fun resendMessageDao(): ResendMessageDao

    companion object {
        private var supportSQLiteDatabase: SupportSQLiteDatabase? = null

        private var INSTANCE: MessageRoomDatabase? = null

        fun getInstance(context: Context): MessageRoomDatabase {
            if (INSTANCE != null)
                return INSTANCE!!
            synchronized(this) {
                INSTANCE = Room.databaseBuilder(context.applicationContext,
                        MessageRoomDatabase::class.java, Constant.DataBase.DB_NAME)
                        .enableMultiInstanceInvalidation()
                        .addCallback(CALLBACK)
                        .fallbackToDestructiveMigration()
                        .build()
                return INSTANCE!!
            }

        }

        @Transaction
        fun checkPoint() {
            supportSQLiteDatabase?.query("PRAGMA wal_checkpoint(FULL)")?.close()
        }

        private val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("CREATE TRIGGER conversation_last_message_update AFTER INSERT ON messages BEGIN UPDATE conversations SET last_message_id = new.id WHERE conversation_id = new.conversation_id; END")
                db.execSQL("CREATE TRIGGER conversation_last_message_delete AFTER DELETE ON messages BEGIN UPDATE conversations SET last_message_id = (select id from messages where conversation_id = old.conversation_id order by created_at DESC limit 1) WHERE conversation_id = old.conversation_id; END")
                db.execSQL("CREATE TRIGGER conversation_unseen_message_count_insert AFTER INSERT ON messages BEGIN UPDATE conversations SET unseen_message_count = (SELECT count(m.id) FROM messages m, users u WHERE m.sender_id = u.user_id AND u.relationship != 'ME' AND m.status = 'DELIVERED' AND conversation_id = new.conversation_id) where conversation_id = new.conversation_id; END")
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                supportSQLiteDatabase = db
                db.execSQL("CREATE TRIGGER IF NOT EXISTS conversation_last_message_update AFTER INSERT ON messages BEGIN UPDATE conversations SET last_message_id = new.id WHERE conversation_id = new.conversation_id; END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS conversation_last_message_delete AFTER DELETE ON messages BEGIN UPDATE conversations SET last_message_id = (select id from messages where conversation_id = old.conversation_id order by created_at DESC limit 1) WHERE conversation_id = old.conversation_id; END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS conversation_unseen_message_count_insert AFTER INSERT ON messages BEGIN UPDATE conversations SET unseen_message_count = (SELECT count(m.id) FROM messages m, users u WHERE m.sender_id = u.user_id AND u.relationship != 'ME' AND m.status = 'DELIVERED' AND conversation_id = new.conversation_id) where conversation_id = new.conversation_id; END")
            }
        }

    }
}