<p>Dobrý den,</p>

<p>dne ${createdDate} ve ${createdTime} byla vygenerována žádost o revizi formátových politik.</p>
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
    S pozdravem
    <br/>
    Aplikace ${appName}
</p>

<p style="color: silver">Tento email byl poslán automatizovaně, proto na něj neodpovídejte.</p>
