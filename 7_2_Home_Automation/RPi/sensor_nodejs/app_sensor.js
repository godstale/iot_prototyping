const express = require('express')
const sql = require('mysql')
const bodyParser = require('body-parser')
const path = require('path')
const mqtt = require('mqtt')
const app = express()

const sqlConn = sql.createConnection({
  host: 'localhost',
  user: 'root',
  password: '12341234',
  database: 'lora_db',
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
port = 3010
app.listen(port, () => {
  console.log("Server has been started");
});