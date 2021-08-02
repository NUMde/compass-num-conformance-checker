$(document).ready(function () {
    $("#resource-form").submit(validateResource);
});

function validateResource(event) {
    event.preventDefault();

    // clear & lock UI
    $("#messages").empty();
    lockUI(true);

    let url = $(this).attr("action");
    let data = $("#resource-form textarea").val();
    let contentType = data.startsWith("<") ? "application/xml" : "application/json"
    return $.ajax({
        type: "POST",
        url: url,
        headers: {
            Accept: "application/json, text/plain; charset=utf-8",
            "Content-Type": contentType
        },
        data: data,
        success: showValidationMessages,
        error: handleError
    });
}

function showValidationMessages(operationOutcome) {
    lockUI(false);
    operationOutcome.issue.forEach((issue) => {
        let severity = issue.severity;

        if (severity === "fatal" || severity === "error") {
            severity = "danger"
        } else if (severity === "information") {
            severity = "info";
        }

        let location = ""
        if (issue.location !== undefined && issue.location.length >= 2) {
            location = `<b>${issue.location[1]}: </b>`
        }

        addMessage(severity, `${location} ${issue.diagnostics}`)
    });
}

function handleError(request) {
    lockUI(false);
    addMessage("danger", `<b>HTTP ${request.status}</b> <pre>${request.responseText}</pre>`)
}

function lockUI(locked) {
    $("#resource-form button, #resource-form textarea").prop("disabled", locked)
    $(".spinner-border").toggleClass("hidden-spinner")
}

function addMessage(severity, message) {
    $("#messages").append(`<div class="mb-3 alert alert-${severity}" role="alert">${message}</div>`);
}
