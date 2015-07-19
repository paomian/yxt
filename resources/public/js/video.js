// Normalize the various vendor prefixed versions of getUserMedia.
var stream = null;
var canvas = document.querySelector('canvas');
var ctx = canvas.getContext('2d');
//var vid = document.getElementById('camera-stream');
var cameraStream = $('#camera-stream');
var vid = cameraStream[0];
var reset = $('#reset');
var submit = $('#submit');


function snapshot() {
    console.log('snapshot');
    submit.removeAttr('disabled');
    reset.removeAttr('disabled');
    //vid.hide();
    cameraStream.hide();
    $('img').show();
    if (stream) {
        ctx.drawImage(vid,0,0,500,375);
        //document.querySelector('img').src = canvas.toDataURL('image/webp');
        $('img').attr('src', canvas.toDataURL('image/webp'));
        //document.getElementById('fimg').value = canvas.toDataURL('image/png').substr(22);
        $('#img').attr('value', canvas.toDataURL('image/png').substr(22));
    }
}

function fsubmit(anti) {
    var tmp = canvas.toDataURL('image/png').substr(22);
    console.log(anti);
    submit.attr('disabled','disabled');
    reset.attr('disabled','disabled');
    $.ajax({
        type: 'POST',
        url: '/yxt',
        beforeSend: function (request) {
            request.setRequestHeader("X-Csrf-Token", anti);
        },
        data: {file:tmp},
        success: function (data) {
            submit.removeAttr('disabled');
            reset.removeAttr('disabled');
            data = JSON.parse(data);
            $('#tmp').remove();
            if(data.error) {
                $("#message").append('<div id="tmp"><p>好像出错了！ ' + data.error + '</p></div>');
                $('#myModal').modal({
                    keyboard: false
                });
            } else {
                var html = '<div id="tmp">';
                for(var key in data) {
                    if(key == 'hello') {
                        html = html+'<p>你是否在 ' + data[key].created_at  + ' 说过：' + data[key].hello + ' </p>';
                    } else {
                        html = html+'<p>你的 ' + key + ' 是 : ' + data[key] + ' </p>';
                    }
                }
                if (data['hello']) {
                    html = html + '<a href="/chat.html" class="btn btn-success" role="button">去聊天</a>'
                    html = html + '<a href="/hello.html" class="btn btn-success" role="button">在说句话吧</a>';
                } else {
                    html = html + '<a href="/chat.html" class="btn btn-success" role="button">去聊天</a>'
                    html = html + '<a href="/hello.html" class="btn btn-success" role="button">你好像还沉默着</a>';
                }
                html = html + '</div>';
                $("#message").append(html);
                $('#myModal').modal({
                    keyboard: false
                });
            }
        },
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
            fsubmit(data);
        },
        error: function (data) {
            console.log(data);
            alert('getToken error');
        },
        dataType: 'text'
    });
}

function freset() {
    submit.attr('disabled','disabled');
    reset.attr('disabled','disabled');
    $('img').hide();
    cameraStream.show();
}

//vid.addEventListener('click', snapshot, false);
cameraStream.click(snapshot);
//console.log($('#submit'));
submit.click(getToken);
reset.click(freset);

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

window.onload = function() {
    console.log("你看我干嘛？");
};
