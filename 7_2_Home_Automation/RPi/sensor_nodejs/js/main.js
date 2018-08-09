/* eslint-disable object-shorthand */

/* global Chart, CustomTooltips, getStyle, hexToRgba */

/**
 * --------------------------------------------------------------------------
 * CoreUI Free Boostrap Admin Template (v2.0.0): main.js
 * Licensed under MIT (https://coreui.io/license)
 * --------------------------------------------------------------------------
 */
/* eslint-disable no-magic-numbers */
// Disable the on-canvas tooltip
Chart.defaults.global.pointHitDetectionRadius = 1
Chart.defaults.global.tooltips.enabled = false
Chart.defaults.global.tooltips.mode = 'index'
Chart.defaults.global.tooltips.position = 'nearest'
Chart.defaults.global.tooltips.custom = CustomTooltips // eslint-disable-next-line no-unused-vars

var drawInterval
var isDrawing = false
var drawingChannel = 0

var data = {
  labels: [],
  datasets: [{
    label: '',
    backgroundColor: hexToRgba(getStyle('--info'), 10),
    borderColor: getStyle('--info'),
    pointHoverBackgroundColor: '#fff',
    borderWidth: 2,
    data: []
  }]
}

var options = {
  maintainAspectRatio: false,
  legend: {
    display: false
  },
  scales: {
    xAxes: [{
      gridLines: {
        drawOnChartArea: false
      }
    }],
    yAxes: [{
      ticks: {
        beginAtZero: true,
        maxTicksLimit: 10,
        stepSize: 30
      }
    }]
  }
}

var ctx = document.getElementById('main-chart')
var mainChart = new Chart(ctx, {
  type: 'line',
  data: data,
  options: options,
  elements: {
    point: {
      radius: 0,
      hitRadius: 10,
      hoverRadius: 4,
      hoverBorderWidth: 3
    }
  }
})

var buttonStop = document.getElementById('stopBtn')
buttonStop.addEventListener('click', ()=>{
  if(isDrawing) {
    stopDraw()
    buttonStop.textContent = 'Start Update'
  }
  else {
    startDraw(drawingChannel)
    buttonStop.textContent = 'Stop Update'
  }
})

var buttonSensorOn = document.getElementById('sensorOn')
buttonSensorOn.addEventListener('click', ()=>{
  changeStatus(drawingChannel, "1")
})

var buttonSensorOff = document.getElementById('sensorOff')
buttonSensorOff.addEventListener('click', ()=>{
  changeStatus(drawingChannel, "0")
})

function startDraw(channel) {
  isDrawing = true
  drawingChannel = channel
  drawInterval = setInterval(() => {
    updateChart(drawingChannel)
  }, 5000)
}

function stopDraw() {
  isDrawing = false
  clearInterval(drawInterval)
}

function drawChart(resultData) {
  var time = resultData.time
  var sensorData = resultData.data
  for (var i = 0; i < sensorData.length; i++) {
    data.datasets[0].data[i] = sensorData[i]
    // data.labels[i] = new Date(time[i]).customFormat('#YYYY#/#MM#/#DD# #hh#:#mm#:#ss#')
    data.labels[i] = new Date(time[i]).customFormat('#YYYY#/#MM#/#DD# #hhh#:#mm#:#ss#')
  }
  var max = sensorData.reduce((num1, num2) => {
    return Math.max(num1, num2) 
  })
  options.scales.yAxes[0].ticks.max = max + 30
  data.datasets[0].label = resultData.channelInfo
  mainChart.update()
}

function updateChart(channel) {
  var oReq = new XMLHttpRequest()

  oReq.open('POST', '/index/'+channel)
  oReq.setRequestHeader('Content-Type', "application/json")
  oReq.send()

  oReq.addEventListener('load', function() {
    var result = JSON.parse(oReq.responseText)
    drawChart(result)
  })
}

function changeStatus(channel, status) {
  var params = {
    status:status
  }

  var oReq = new XMLHttpRequest()

  oReq.open('POST', '/index/'+channel+"/changeStatus")
  oReq.setRequestHeader('Content-Type', "application/json")
  console.log(JSON.stringify(params))
  oReq.send(JSON.stringify(params))

  oReq.addEventListener('load', function() {
    alert("장치 상태를 변경하였습니다.")
  })
}