import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import coil.compose.AsyncImage
import com.example.weatherapp.data.HourModel
import com.example.weatherapp.data.WeatherModel
import com.example.weatherapp.ui.theme.BlueLight
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun MainList(list: List<WeatherModel>, currentDay: MutableState<WeatherModel>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(
            list
        ) { _, item ->
            ListItem(item, currentDay)
        }
    }
}

@Composable
fun ListItem(item: WeatherModel, currentDay: MutableState<WeatherModel>) {
    var showDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp)
            .clickable {
                if (item.hours.isEmpty()) return@clickable
                currentDay.value = item
                showDialog = true
            },
        backgroundColor = BlueLight,
        elevation = 0.dp,
        shape = RoundedCornerShape(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 8.dp,
                    top = 5.dp,
                    bottom = 5.dp
                )
            ) {
                Text(text = item.time)
                Text(
                    text = item.condition,
                    color = Color.White
                )
            }
            Text(
                text = item.currentTemp.ifEmpty { "${item.maxTemp}/${item.minTemp}" },
                color = Color.White,
                style = TextStyle(fontSize = 25.sp)
            )
            AsyncImage(
                model = "https:${item.icon}",
                contentDescription = "im5",
                modifier = Modifier
                    .padding(
                        end = 8.dp
                    )
                    .size(35.dp)
            )
        }
        if (showDialog) {
            AlertDialog(
                modifier = Modifier.fillMaxWidth(),
                onDismissRequest = {
                    showDialog = false
                },
                title = {
                    Text(text = "Информация о погоде")
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()

                    ) {
                        Text(text = "Минимальная температура за день: ${item.minTemp}")
                        Text(text = "Максимальная температура за день: ${item.maxTemp}")
                        Text(text = "Прогноз на день: ${item.condition}")
                        Spacer(modifier = Modifier.height(16.dp))
                        val originalData = getLineChartData(parseHourModels(item.hours))

                        val step = originalData.size / 8  // Определение шага между элементами

                        val reducedData = mutableListOf<Point>()

                        for (i in 0 until originalData.size step step) {
                            reducedData.add(originalData[i])
                        }

                        val pointsData = reducedData.toList()

                        val xAxisData = AxisData.Builder()
                            .axisStepSize(10.dp)
                            .steps(pointsData.size - 1)
                            .labelData { i ->
                                pointsData[i / 3].x.toInt().toString()
                            }
                            .build()

                        val yAxisData = AxisData.Builder()
                            .steps(pointsData.size - 1)
                            .backgroundColor(Color.Transparent)
                            .labelData { i ->
                                if (i < pointsData.size) {
                                    pointsData[i].y.toString()
                                } else {
                                    ""
                                }
                            }
                            .build()
                        val data = LineChartData(
                            LinePlotData(
                                lines = listOf(
                                    Line(
                                        dataPoints = pointsData,
                                        LineStyle(color = BlueLight),
                                        IntersectionPoint(color = BlueLight),
                                        SelectionHighlightPoint(),
                                        ShadowUnderLine(
                                            alpha = 0.5f,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    BlueLight,
                                                    Color.Transparent
                                                )
                                            )
                                            ),
                                        SelectionHighlightPopUp()
                                    )
                                )
                            ),
                            xAxisData = xAxisData,
                            yAxisData = yAxisData
                        )
                        LineChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            lineChartData = data
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDialog = false
                        }
                    ) {
                        Text("Закрыть")
                    }
                }
            )
        }
    }
}
fun parseHourModels(jsonString: String): List<HourModel> {
    val hoursArray = JSONArray(jsonString)

    val hourModels = mutableListOf<HourModel>()

    for (i in 0 until hoursArray.length()) {
        val hourObject = hoursArray.getJSONObject(i)
        val time = hourObject.getString("time")
        val temp = hourObject.getDouble("temp_c")
        val hourModel = HourModel(time, temp)
        hourModels.add(hourModel)
    }

    return hourModels
}
private fun getLineChartData(hours: List<HourModel>): List<Point> {
    val list = mutableListOf<Point>()

    for ((index, hourModel) in hours.withIndex()) {
        // Предположим, что у HourModel есть свойство temp, представляющее температуру
        val temperature = hourModel.temp.toFloat()
        list.add(Point(index.toFloat(), temperature))
    }

    return list
}

@Composable
fun DialogSearch(dialogState: MutableState<Boolean>, onSubmit: (String) -> Unit) {
    val dialogText = remember {
        mutableStateOf("")
    }
    AlertDialog(onDismissRequest = {
        dialogState.value = false
    },
        confirmButton = {
            TextButton(onClick = {
                onSubmit(dialogText.value)
                dialogState.value = false
            }) {
                Text(text = "OK")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                dialogState.value = false
            }) {
                Text(text = "Cancel")
            }
        },
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Введите город:")
                TextField(value = dialogText.value, onValueChange = {
                    dialogText.value = it
                })
            }
        }
    )
}