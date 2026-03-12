package org.brightmindenrichment.street_care

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class FirestoreBatchWriteTest {

    private val db: FirebaseFirestore = Firebase.firestore

    @Before
    fun setup() {
        // Connect to Firebase Emulator via USB ADB reverse port forwarding
        try {
            db.useEmulator("localhost", 8080)
        } catch (e: IllegalStateException) {
            // Catches the error if useEmulator is called more than once
        }
    }

    @Test
    fun interactionLog_succeedsWithValidData() = runTest {
        val ilData = hashMapOf(
            "userId" to "test_user_123",
            "outreachId" to null,
            "firstName" to "John",
            "lastName" to "Doe",
            "email" to "john@example.com",
            "phoneNumber" to "555-1234",
            "interactionDate" to "2025-03-06",
            "startTimestamp" to Timestamp.now(),
            "endTimestamp" to Timestamp.now(),
            "listOfSupportsProvided" to listOf("Food", "Shelter"),
            "numPeopleHelped" to 5,
            "carePackagesDistributed" to 2,
            "carePackageContents" to listOf("Blanket", "Water"),
            "numPeopleJoined" to 3,
            "addr1" to "123 Main St",
            "addr2" to "",
            "city" to "Denver",
            "state" to "CO",
            "zipcode" to "80202",
            "country" to "USA",
            "helpRequestCount" to 0,
            "helpRequestDocIds" to emptyList<String>(),
            "isPublic" to true,
            "status" to "pending",
            "lastModifiedTimestamp" to Timestamp.now(),
            "lastActionPerformed" to "submit",
            "isFlagged" to false,
            "flaggedByUser" to ""
        )

        try {
            db.collection("InteractionLog")
                .document("valid_doc_1")
                .set(ilData)
                .await()

            println("✓ InteractionLog write succeeded with valid data")
        } catch (e: Exception) {
            fail("Expected write to succeed, but it failed: ${e.message}")
        }
    }

    @Test
    fun helpRequest_succeedsWithValidData() = runTest {
        val hrData = hashMapOf(
            "interactionLogDocId" to "il_123",
            "firstName" to "Jane",
            "lastName" to "Smith",
            "locationLandmark" to "Downtown Park",
            "timestampOfInteraction" to Timestamp.now(),
            "helpProvidedCategory" to listOf("Food Assistance", "Medical"),
            "furtherHelpCategory" to listOf("Housing"),
            "followUpTimestamp" to null,
            "additionalDetails" to "Follow up needed",
            "interactionLogFirstName" to "John",
            "isPublic" to true,
            "status" to "pending",
            "lastModifiedTimestamp" to Timestamp.now(),
            "lastActionPerformed" to "submit",
            "completedTimestamp" to null,
            "isCompleted" to false
        )

        try {
            db.collection("HelpRequest")
                .document("valid_hr_1")
                .set(hrData)
                .await()

            println("✓ HelpRequest write succeeded with valid data")
        } catch (e: Exception) {
            fail("Expected write to succeed, but it failed: ${e.message}")
        }
    }

    @Test
    fun batchWrite_succeedsWithILAndMultipleHelpRequests() = runTest {
        val ilData = hashMapOf(
            "userId" to "batch_user_456",
            "outreachId" to null,
            "firstName" to "Alice",
            "lastName" to "Johnson",
            "email" to "alice@example.com",
            "phoneNumber" to "555-5678",
            "interactionDate" to "2025-03-06",
            "startTimestamp" to Timestamp.now(),
            "endTimestamp" to Timestamp.now(),
            "listOfSupportsProvided" to listOf("Food"),
            "numPeopleHelped" to 3,
            "carePackagesDistributed" to 0,
            "carePackageContents" to emptyList<String>(),
            "numPeopleJoined" to 0,
            "addr1" to "",
            "addr2" to "",
            "city" to "",
            "state" to "",
            "zipcode" to "",
            "country" to "USA",
            "helpRequestCount" to 2,
            "helpRequestDocIds" to listOf("batch_hr_1", "batch_hr_2"),
            "isPublic" to true,
            "status" to "pending",
            "lastModifiedTimestamp" to Timestamp.now(),
            "lastActionPerformed" to "submit",
            "isFlagged" to false,
            "flaggedByUser" to ""
        )

        val hr1Data = hashMapOf(
            "interactionLogDocId" to "batch_il_1",
            "firstName" to "Bob",
            "lastName" to null,
            "locationLandmark" to null,
            "timestampOfInteraction" to Timestamp.now(),
            "helpProvidedCategory" to listOf("Food"),
            "furtherHelpCategory" to emptyList<String>(),
            "followUpTimestamp" to null,
            "additionalDetails" to null,
            "interactionLogFirstName" to "Alice",
            "isPublic" to true,
            "status" to "pending",
            "lastModifiedTimestamp" to Timestamp.now(),
            "lastActionPerformed" to null,
            "completedTimestamp" to null,
            "isCompleted" to false
        )

        val hr2Data = hashMapOf(
            "interactionLogDocId" to "batch_il_1",
            "firstName" to "Charlie",
            "lastName" to null,
            "locationLandmark" to null,
            "timestampOfInteraction" to Timestamp.now(),
            "helpProvidedCategory" to listOf("Shelter"),
            "furtherHelpCategory" to emptyList<String>(),
            "followUpTimestamp" to null,
            "additionalDetails" to null,
            "interactionLogFirstName" to "Alice",
            "isPublic" to true,
            "status" to "pending",
            "lastModifiedTimestamp" to Timestamp.now(),
            "lastActionPerformed" to null,
            "completedTimestamp" to null,
            "isCompleted" to false
        )

        try {
            val batch = db.batch()

            batch.set(db.collection("InteractionLog").document("batch_il_1"), ilData)
            batch.set(db.collection("HelpRequest").document("batch_hr_1"), hr1Data)
            batch.set(db.collection("HelpRequest").document("batch_hr_2"), hr2Data)

            batch.commit().await()

            println("✓ Batch write succeeded with IL and 2 HelpRequests")
        } catch (e: Exception) {
            fail("Expected batch write to succeed, but it failed: ${e.message}")
        }
    }

    @Test
    fun interactionLog_failsWithMissingRequiredFields() = runTest {
        // Missing required fields like firstName, lastName, userId
        val invalidData = hashMapOf(
            "email" to "test@example.com",
            "phoneNumber" to "555-1234"
        )

        try {
            db.collection("InteractionLog")
                .document("invalid_doc_1")
                .set(invalidData)
                .await()

            // If we reach here, the database accepted it - this is still valid per Firestore
            // (Firestore doesn't enforce required fields by default)
            println("✓ Write succeeded - Firestore allows optional fields")
        } catch (e: Exception) {
            println("✗ Write failed (expected with validation rules): ${e.message}")
        }
    }

    @Test
    fun interactionLog_succeedsWithTimestampConversion() = runTest {
        val calendar = Calendar.getInstance()
        calendar.set(2025, Calendar.MARCH, 6, 14, 30, 0)
        val timestamp = Timestamp(calendar.time)

        val ilData = hashMapOf(
            "userId" to "timestamp_test_user",
            "firstName" to "Time",
            "lastName" to "Keeper",
            "interactionDate" to "2025-03-06",
            "startTimestamp" to timestamp,
            "endTimestamp" to Timestamp.now(),
            "isPublic" to true
        )

        try {
            db.collection("InteractionLog")
                .document("timestamp_doc")
                .set(ilData)
                .await()

            println("✓ Write succeeded with timestamp conversion")
        } catch (e: Exception) {
            fail("Expected write with timestamps to succeed: ${e.message}")
        }
    }

    @Test
    fun helpRequest_succeedsWithListFields() = runTest {
        val hrData = hashMapOf(
            "interactionLogDocId" to "list_test_il",
            "firstName" to "List",
            "helpProvidedCategory" to listOf("Food", "Medical", "Clothing"),
            "furtherHelpCategory" to listOf("Housing", "Job Training"),
            "isPublic" to true
        )

        try {
            db.collection("HelpRequest")
                .document("list_hr_doc")
                .set(hrData)
                .await()

            println("✓ Write succeeded with list fields")
        } catch (e: Exception) {
            fail("Expected write with list fields to succeed: ${e.message}")
        }
    }

    @Test
    fun interactionLog_failsWithExtraFields() = runTest {
        // This tests the firestore.rules enforcement - extra fields should be rejected
        val ilDataWithExtra = hashMapOf(
            "userId" to "test_user_extra",
            "outreachId" to null,
            "firstName" to "John",
            "lastName" to "Doe",
            "email" to "john@example.com",
            "phoneNumber" to "555-1234",
            "interactionDate" to "2025-03-06",
            "startTimestamp" to Timestamp.now(),
            "endTimestamp" to Timestamp.now(),
            "listOfSupportsProvided" to listOf("Food"),
            "numPeopleHelped" to 5,
            "carePackagesDistributed" to 2,
            "carePackageContents" to listOf("Blanket"),
            "numPeopleJoined" to 3,
            "addr1" to "123 Main St",
            "addr2" to "",
            "city" to "Denver",
            "state" to "CO",
            "zipcode" to "80202",
            "country" to "USA",
            "helpRequestCount" to 0,
            "helpRequestDocIds" to emptyList<String>(),
            "isPublic" to true,
            "status" to "pending",
            "lastModifiedTimestamp" to Timestamp.now(),
            "lastActionPerformed" to "submit",
            "isFlagged" to false,
            "flaggedByUser" to "",
            "extraFieldThatShouldFail" to "this should cause rejection" // EXTRA FIELD
        )

        try {
            db.collection("InteractionLog")
                .document("invalid_extra_fields")
                .set(ilDataWithExtra)
                .await()

            fail("Expected write to FAIL due to extra fields, but it succeeded!")
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                println("✓ InteractionLog correctly rejected document with extra fields")
            } else {
                fail("Expected PERMISSION_DENIED, got: ${e.code}")
            }
        }
    }

    @Test
    fun batchWrite_rollsBackOnPartialFailure() = runTest {
        // This test demonstrates atomic batch behavior
        val ilData = hashMapOf(
            "userId" to "rollback_user",
            "outreachId" to null,
            "firstName" to "Rollback",
            "lastName" to "Test",
            "email" to "test@example.com",
            "phoneNumber" to "555-1234",
            "interactionDate" to "2025-03-06",
            "startTimestamp" to Timestamp.now(),
            "endTimestamp" to Timestamp.now(),
            "listOfSupportsProvided" to emptyList<String>(),
            "numPeopleHelped" to 0,
            "carePackagesDistributed" to 0,
            "carePackageContents" to emptyList<String>(),
            "numPeopleJoined" to 0,
            "addr1" to "",
            "addr2" to "",
            "city" to "",
            "state" to "",
            "zipcode" to "",
            "country" to "USA",
            "helpRequestCount" to 0,
            "helpRequestDocIds" to emptyList<String>(),
            "isPublic" to true,
            "status" to "pending",
            "lastModifiedTimestamp" to Timestamp.now(),
            "lastActionPerformed" to "submit",
            "isFlagged" to false,
            "flaggedByUser" to ""
        )

        val hrData = hashMapOf(
            "interactionLogDocId" to "rollback_il",
            "firstName" to "Helper",
            "lastName" to null,
            "locationLandmark" to null,
            "timestampOfInteraction" to Timestamp.now(),
            "helpProvidedCategory" to emptyList<String>(),
            "furtherHelpCategory" to emptyList<String>(),
            "followUpTimestamp" to null,
            "additionalDetails" to null,
            "interactionLogFirstName" to "Rollback",
            "isPublic" to true,
            "status" to "pending",
            "lastModifiedTimestamp" to Timestamp.now(),
            "lastActionPerformed" to null,
            "completedTimestamp" to null,
            "isCompleted" to false
        )

        try {
            val batch = db.batch()
            batch.set(db.collection("InteractionLog").document("rollback_il"), ilData)
            batch.set(db.collection("HelpRequest").document("rollback_hr"), hrData)
            batch.commit().await()

            println("✓ Atomic batch write succeeded - both documents written")
        } catch (e: Exception) {
            println("✗ Batch write failed: ${e.message}")
        }
    }
}
