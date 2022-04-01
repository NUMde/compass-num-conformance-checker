$(document).ready(function () {
    $("#resource-form").submit(validateResource);

    $("form button[type=submit]").click(function() {
        $("button[type=submit]", $(this).parents("form")).removeAttr("clicked");
        $(this).attr("clicked", "true");
    });
});

function validateResource(event) {
    event.preventDefault();
    // clear
    $("#messages").empty();
    var val = $("button[type=submit][clicked=true]").val();
    if(val==="validate"){

        // lock UI
        lockUI(true,"validate");

        let url = "/fhir/$validate-single"
        let data = $("#resource-form textarea").val();
        let contentType = data.startsWith("<") ? "application/xml" : "application/json"
        return $.ajax({
            type: "POST",
            url: url,
            headers: {
                Accept: "application/json, application/pdf, text/plain; charset=utf-8",
                "Content-Type": contentType
            },
            data: data,
            success: showValidationMessages,
            error: handleError
        });
    } else{
        lockUI(true,"gecco");

        let url = "/fhir/$check-gecco-conformance";
        let data = $("#resource-form textarea").val();
        let contentType = data.startsWith("<") ? "application/xml" : "application/json"
        fetch(url, {
            method: "POST",
            headers: {
                Accept: "application/json, application/pdf, text/plain; charset=utf-8",
                "Content-Type": contentType
            },
            body: data
        }).then(
            res => res.headers.get("Content-Type").includes("pdf") ? res.blob() : res.json()
        ).then( res => {
                if(res instanceof Blob) {
                    lockUI(false);
                    var a = document.createElement("a");
                    a.href = URL.createObjectURL(res);
                    a.download = "certificate.pdf";
                    document.body.appendChild(a);
                    a.click();
                } else {
                    showValidationMessages(res);
                }
            }
        ).catch(err => {
            lockUI(false)
            addMessage("danger", `${err}`)
        })


    }

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

function lockUI(locked, action="") {
    if(locked === false){
        $("#resource-form button, #resource-form textarea").prop("disabled", locked)
        if(!$("#validate-spinner").hasClass("hidden-spinner")){
            $("#validate-spinner").addClass("hidden-spinner");
        }
        else if (!$("#gecco-spinner").hasClass("hidden-spinner")){
            $("#gecco-spinner").addClass("hidden-spinner");
        }
    } else {
        $("#resource-form button, #resource-form textarea").prop("disabled", locked)
        $("#" + action + "-spinner").toggleClass("hidden-spinner")
    }
}

function addMessage(severity, message) {
    $("#messages").append(`<div class="mb-3 alert alert-${severity}" role="alert">${message}</div>`);
}
