// Normalize the various vendor prefixed versions of getUserMedia.
var stream = null;
var canvas = document.querySelector('canvas');
var ctx = canvas.getContext('2d');
//var vid = document.getElementById('camera-stream');
var cameraStream = $("#camera-stream");
var vid = cameraStream[0];


function snapshot() {
    console.log('snapshot');
    if (stream) {
        ctx.drawImage(vid,0,0,500,375);
        document.querySelector('img').src = canvas.toDataURL('image/webp');
        //$('#user').attr('src', canvas.toDataURL('image/webp'));
        document.getElementById('fimg').value = canvas.toDataURL('image/png').substr(22);
        //$('#user').attr('value', canvas.toDataURL('image/png').substr(22));
    }
}

function submit(anti) {
    var tmp = canvas.toDataURL('image/png').substr(22);
    console.log(anti);
    $.ajax({
        type: 'POST',
        url: '/yxt',
        beforeSend: function (request) {
            request.setRequestHeader("X-Csrf-Token", anti);
        },
        data: {file:tmp},
        success: function (data) {return data;},
        error: function (data) {
            console.log(data);
            alert('submit error');
        },
        dataType: 'html'
    });
}

function getToken() {
    $.ajax({
        type: 'GET',
        url: '/token',
        success: function (data) {
            submit(data);
        },
        error: function (data) {
            console.log(data);
            alert('getToken error');
        },
        dataType: 'text'
    });
}

//vid.addEventListener('click', snapshot, false);
cameraStream.click(snapshot);
//console.log($('#submit'));
$('#submit').click(getToken);

navigator.getUserMedia = navigator.getUserMedia ||
    navigator.webkitGetUserMedia ||
    navigator.mozGetUserMedia ||
    navigator.msGetUserMedia;
if (navigator.getUserMedia) {
    // Request the camera.
    navigator.getUserMedia(
        // Constraints
        {video: true},
        // Success Callback
        function(localMediaStream) {
            // Create an object URL for the video stream and use this
            // to set the video source.
            vid.src = window.URL.createObjectURL(localMediaStream);
            stream = localMediaStream;
        },
        // Error Callback
        function(err) {
            // Log the error to the console.
            console.log('The following error occurred when trying to use getUserMedia: ' + err);
        }
    );
} else {
    alert('Sorry, your browser does not support getUserMedia');
}
