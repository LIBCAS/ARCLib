<p>Hello,</p>

<p>in ${createdDate} at ${createdTime} a request for revision of format politics has been created.</p>
<p>${result}</p>
<div>
    <#list formats>
        <ul>
            <#items as format>
                <li>${format}</li>
            </#items>
        </ul>
    </#list>
</div>

<p>
    Best regards,
    <br/>
    ${appName} application
</p>

<p style="color: silver">This is an automated message, please do not reply.</p>
