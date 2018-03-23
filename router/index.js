var express = require('express');
 var app = express();
 var router = express.Router();
 var path = require('path');

 // router
 var main = require('./main/main');
 var email = require('./email/email');
 var join = require('./join/index');

 // route URL
 router.get('/', function(req,res){
     console.log('indexjs / path loaded');
     res.sendFile(path.join(__dirname + "/../public/main.html"));
 });

 //set router
 router.use('/main', main);
 router.use('/email', email);
 router.use('/join', join);


 module.exports = router;