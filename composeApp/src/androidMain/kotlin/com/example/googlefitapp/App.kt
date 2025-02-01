package com.example.googlefitapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.DataUpdateRequest
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import googlefitmocker.composeapp.generated.resources.Res
import googlefitmocker.composeapp.generated.resources.compose_multiplatform

@Composable
@Preview
fun App() {
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var stepCount by remember { mutableStateOf(0) }
    val context = LocalContext.current
    
    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { startDate = selectDate(context) }) {
            Text("Select Start Date: $startDate")
        }
        Button(onClick = { endDate = selectDate(context) }) {
            Text("Select End Date: $endDate")
        }
        OutlinedTextField(
            value = stepCount.toString(),
            onValueChange = { stepCount = it.toIntOrNull() ?: 0 },
            label = { Text("Enter Step Count") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { writeStepData(context, startDate, endDate, stepCount) }) {
            Text("Submit")
        }
    }
}

fun writeStepData(context: Context, startDate: LocalDate, endDate: LocalDate, stepCount: Int) {
    val account = GoogleSignIn.getLastSignedInAccount(context) ?: return
    val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    
    val dataSource = DataSource.Builder()
        .setAppPackageName(context.packageName)
        .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
        .setType(DataSource.TYPE_RAW)
        .build()
    
    val dataSet = DataSet.builder(dataSource).apply {
        for (date in startDate.datesUntil(endDate.plusDays(1))) {
            val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dataPoint = DataPoint.builder(dataSource)
                .setTimeInterval(dateMillis, dateMillis + TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS)
                .setField(DataType.TYPE_STEP_COUNT_DELTA.fields[0], stepCount)
                .build()
            add(dataPoint)
        }
    }.build()
    
    Fitness.getHistoryClient(context, account)
        .insertData(dataSet)
        .addOnSuccessListener { Log.i("GoogleFit", "Successfully added step data") }
        .addOnFailureListener { Log.e("GoogleFit", "Failed to add step data", it) }
}
