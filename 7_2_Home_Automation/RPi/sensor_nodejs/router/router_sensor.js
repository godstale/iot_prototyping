var channel = 0
var authCode = ""

module.exports = function (express, sqlConn, mqtt) {
  var route = express.Router()
  require('../js/date_prototype')
  require('../js/util')()
  var fs = require('fs')
  var config = JSON.parse(fs.readFileSync(__dirname + '/../config.json'))

  route.get('/', (req, res) => {
    channel = 0
    authCode = ""
    if (authCode.length == 0) {
      res.redirect('/login')
    }
    else {
      res.render('pages/index', { basedir: './views_file/' })
    }
  })

  route.get('/login', (req, res) => {
    channel = 0
    authCode = ""
    res.render('pages/login', { basedir: './views_file/' })
  })

  route.post('/login/:channel', (req, res) => {
    var responseData = {}
    var query = 'SELECT * FROM `channel_table` WHERE `channel`=? AND `auth_code`=?'

    channel = req.body.channel
    authCode = req.body.authCode

    var params = [channel, authCode]
    sqlConn.query(query, params, (err, rows) => {
      if (err) throw err
      if (rows[0]) {
        responseData.result = "S001"
        responseData.msg = "정상 로그인 되었습니다."
      }
      else {
        responseData.result = "E001"
        responseData.msg = "로그인 실패하였습니다."
      }
      res.json(responseData)
    })    
  })

  route.get('/register', (req, res) => {
    res.render('pages/register', { basedir: './views_file/' })
  })

  route.post('/register', (req, res) => {
    var responseData = {}
    var channelName = req.body.channelName
    var authCode = makeid()
    var query = "INSERT INTO `channel_table`(`channel_info`, `auth_code`) VALUES(?, ?)"
    var params = [channelName, authCode]
    sqlConn.query(query, params, (err, row, fields) => {
      if (err) throw err
      else {
        query = 'SELECT `channel` FROM `channel_table` ORDER BY `channel` DESC LIMIT 1'
        sqlConn.query(query, (err, row, fields) => {
          if(err) throw err
          else {
            responseData.result = "S001"
            responseData.msg = "정상적으로 등록되었습니다."
            responseData.channel = row[0].channel
            responseData.authCode = authCode
            res.json(responseData)
          }
        })
      }
    })
  })

  route.get('/index/:channel', (req, res) => {
    if(isNaN(req.params.channel) || authCode.length == 0) {
      var msg = ""
      if(isNaN(req.params.channel)) {
        msg = "채널을 입력하지 않으셨어요"
      }
      else if(authCode.length == 0) {
        msg = "인증 코드를 입력하지 않으셨어요"
      }
      res.render('pages/nochannel', { msg : msg })
    }
    else {
      var responseData = {}
      var query = 'SELECT * FROM (\
        SELECT `_id`, `data`.`channel`, `channel_info`, `data`, `time`, `status` FROM `data_table` AS `data` INNER JOIN `channel_table` AS `ch` \
        ON `data`.`channel` = `ch`.`channel` WHERE `data`.`channel`=? ORDER BY `_id` DESC LIMIT 10)\
        AS `result` ORDER BY `result`.`_id` ASC'
      var params = [req.params.channel]

      sqlConn.query(query, params, (err, rows) => {
        responseData.time = []
        responseData.data = []
        if (err) throw err
        if (rows[0]) {
          responseData.result = "ok"
          rows.forEach(function (val) {
            responseData.time.push(val.time)
            responseData.data.push(val.data)
          })
          responseData.status = rows[0].status
          responseData.channelInfo = rows[0].channel_info
          var date = new Date().customFormat('#YYYY#/#MM#/#DD#')
          res.render('pages/index', { sensorInfo : rows[0].channel_info, responseData : JSON.stringify(responseData), 
            channel : req.params.channel, data : date })
        }
        else {
          query = 'SELECT * FROM `channel_table` WHERE `channel`=? AND `auth_code`=?'
          var params = [channel, authCode]
          sqlConn.query(query, params, (err, rows) => {
            var date = new Date().customFormat('#YYYY#/#MM#/#DD#')
            res.render('pages/index', { sensorInfo : rows[0].channel_info, responseData : JSON.stringify(responseData), 
              channel : req.params.channel, data : date })
          })
        }
      })
    }
  })

  route.post('/index/:channel', function (req, res) {
    if(isNaN(req.params.channel)) {
      res.render('pages/404', { basedir: './views_file/' })
    }
    else {
      var responseData = {}
      var query = 'SELECT * FROM (\
        SELECT `_id`, `data`.`channel`, `channel_info`, `data`, `time`, `status` FROM `data_table` AS `data` INNER JOIN `channel_table` AS `ch` \
        ON `data`.`channel` = `ch`.`channel` WHERE `data`.`channel`=? ORDER BY `_id` DESC LIMIT 10)\
        AS `result` ORDER BY `result`.`_id` ASC'
      var params = [req.params.channel]
      sqlConn.query(query, params, (err, rows) => {
        responseData.time = []
        responseData.data = []
        if (err) throw err
        if (rows[0]) {
          responseData.result = "ok"
          rows.forEach(function (val) {
            responseData.time.push(val.time)
            responseData.data.push(val.data)
          })
          responseData.status = rows[0].status
          responseData.channelInfo = rows[0].channel_info
        }
        else {
          responseData.result = "none"
          responseData.data = ""
        }
        res.json(responseData)          
      })
    }
  })

  route.post('/index/:channel/changeStatus', function (req, res) {
    var query = 'UPDATE `channel_table` SET `status`=? WHERE `channel`=?'
    var params = [req.body.status, req.params.channel]
    sqlConn.query(query, params, (err, rows) => {
      if (err) throw err
      else {
        var option = {
          port: config.mqtt_port
        }
        var client = mqtt.connect(config.mqtt_addr, option)
        client.on('connect', () => {
          var topic = req.params.channel + '/status'
          client.publish(topic, req.body.status)
          client.end()
        })
        res.status(200).json({ result : "S" })
      }
    })
  })

  route.post('/data', (req, res) => {
    var responseData = {}
    var channel = req.body.channel
    var authCode = req.body.auth_code
    var data = req.body.data
    var query = 'SELECT `status` FROM `channel_table` WHERE `channel`=? AND `auth_code`=?'
    params = [channel, authCode]
    sqlConn.query(query, params, (err, channelRow, fields) => {
      if (err) {
        responseData.result = "E003"
        responseData.msg = 'Internal server error'
        res.status(500).json(responseData)
      }
      else {
        if(channelRow[0]) {
          query = 'INSERT INTO `data_table`(`channel`, `data`) VALUES(?, ?)'
          params = [channel, data]
          var status = channelRow[0].status
          sqlConn.query(query, params, (err, row, fields) => {
            if (err) {
              responseData.result = "E003"
              responseData.msg = 'Internal server error'
              res.status(500).json(responseData)
            }
            else {
              responseData.result = "S003"
              responseData.msg = 'Data registered successfully'
              res.status(200).json(responseData)
            }
          })
        }
        else {
          responseData.result = "E003"
          responseData.msg = 'Channel not registered'
          res.status(500).json(responseData)
        }
      }
    })  
  })

  route.post('/recent_data', (req, res) => {
    var responseData = {}
    var channel = req.body.channel
    var authCode = req.body.auth_code
    var num = req.body.num
    if (num > 100) {
      num = 100
    }

    var query = 'SELECT `_id`, `data`, `time` FROM (\
      SELECT `_id`, `data`.`channel`, `channel_info`, `data`, `time`, `status` FROM `data_table` AS `data` INNER JOIN `channel_table` AS `ch` \
      ON `data`.`channel` = `ch`.`channel` WHERE `data`.`channel`=?  AND `ch`.`auth_code`=? ORDER BY `_id` DESC LIMIT ?)\
      AS `result` ORDER BY `result`.`_id` ASC'
    params = [channel, authCode, num]
    sqlConn.query(query, params, (err, channelRow, fields) => {
      if (err) {
        responseData.result = "E003"
        responseData.msg = 'Internal server error'
        res.status(500).json(responseData)
      }
      else {
        if(channelRow[0]) {
            responseData.result = "S003"
            responseData.msg = 'Get data successfully'
            responseData.list = channelRow
            res.status(200).json(responseData)
        }
        else {
          responseData.result = "E003"
          responseData.msg = 'Channel not registered'
          res.status(500).json(responseData)
        }
      }
    })  
  })

  return route
}