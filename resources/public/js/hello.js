console.log('hello world');
var submit = $('#submit');
window.onload = function() {
    var hello = $('#hello');
    console.log(hello.text());
    console.log('hello world');
};

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

function fsubmit(token) {
    var thello = $('#hello').text();
    $.ajax({
        type: 'POST',
        url: '/hello',
        beforeSend: function (request) {
            request.setRequestHeader("X-Csrf-Token", token);
        },
        data: {hello:thello},
        success: function (data) {
            alert(data);
        },
        error: function (data) {
            console.log(data.responseText);
            alert(data.responseText);
        }
    });
}

submit.click(getToken);
