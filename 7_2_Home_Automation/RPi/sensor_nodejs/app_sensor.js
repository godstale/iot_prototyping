const express = require('express')
const sql = require('mysql')
const bodyParser = require('body-parser')
const path = require('path')
const mqtt = require('mqtt')
var fs = require('fs')

const app = express()
var config = JSON.parse(fs.readFileSync(__dirname + '/config.json'))

const sqlConn = sql.createConnection({
  host: 'localhost',
  user: 'root',
  password: 'user_passwd',
  database: 'sensor_db',
})
sqlConn.connect()

app.locals.pretty = true
app.set('views', './views_file')
app.set('view engine', 'pug')
// app.set('views', path.join(__dirname, 'pug'));
// app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json())
app.use(express.static('public'))
app.use('/script', express.static('js'))
app.use('/node_modules', express.static(path.join(__dirname, '/node_modules')))

var routeLora = require(path.join(__dirname, 'router', 'router_sensor'))(express, sqlConn, mqtt)
app.use('/', routeLora)
app.listen(config.server_port, () => {
  console.log("Server has been started");
});