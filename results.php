<html>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">
<style>
  * {
      box-sizing: border-box;
  }

  form input[type=text] 
    {
      padding: 10px;
      font-size: 17px;
      border: 1px solid grey;
      float: left;
      width: 80%;
    }

  form button 
  {
     float: left;
     width: 20%;
     padding: 11px;
     background: red;
     color: white;
     font-size: 17px;
     border: 1px solid grey;
     border-left: none;
     cursor: pointer;
  }
</style>
<body mariginwidth="2">
<table width=6% cellpadding=5 border=1 bgcolor=red onclick="location.href='http://csce.uark.edu/~alprakas/information_retrieval/query.php'">
  <tr>
  <td align=center>
  <font size=+2 style=times color=white>Query</font>
  </td>
  </tr>
</table>
<br>
<form action="results.php" method="post" style="max-width:300px">
    <input type="text" name="query_terms" placeholder="Search..." value="<?php echo isset($_POST['query_terms']) ? $_POST['query_terms'] : '' ?>">
    <button type="submit" name="submit">
         <i class="fa fa-search"></i>
    </button>
</form>
<br>
<br>
</body>
</html>

<?php
$query_terms = ($_POST["query_terms"]);

if (isset($_POST['submit'])) 
{
    $query_terms = escapeshellarg($_POST[query_terms]);

    $command = 'java -cp .:jsoup-1.14.2.jar query ' . $query_terms . ' ' .output_files300;

    $escaped_command = escapeshellcmd($command);
    system($escaped_command);           
}
?>