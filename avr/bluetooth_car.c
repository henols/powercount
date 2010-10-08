/* Bluetooth Controlled Car
Accpets commands through UART, via GP-GC021 Bluetooth Module
Control of head lights, brake lights, one drive motor and one
steering servo are implemented.
Clock frequency is 4MHz
*/

#define HDLED PORTB.B7
#define BRKLED PORTB.B6
#define M1p PORTB.B5
#define M1m PORTB.B4
#define SERVO PORTB.B3

unsigned short uart_rd;
char pos;

// Posistion Servo Rotor
void position(){
     char i;
     SERVO = 1;
     // create ON-time delay in 10ths of a ms, delay in for-loop, hence less than 100ms
     for (i=0; i<=pos; i++) Delay_us(85);
     SERVO = 0;
     Delay_ms(12);
}

// Write outputs
void action(){
// simple error check
   if ((uart_rd & 0b00000011)>0){        // faulty byte received, since last two bits aren't used
      // clear outputs, center servo
      PORTB = 0;
      UART1_Write(0b00000011);           // send error, ie faulty bits
      pos = 14;                          // compensate for off-center rotor placement, 1.5ms should be standard
   }else{
      // write outputs, place servo
      UART1_Write(uart_rd);              // send response
      if ((uart_rd & 0b00001000)>0){
           pos = 9;
      }else if ((uart_rd & 0b00000100)>0){
           pos = 18;
      }else{
           pos = 14;                      // compensate for off-center rotor placement
      }
      PORTB = uart_rd & 0xF0;
   }
}

// Read UART and call action
void main() {
     UART1_Init(9600);                   // Initialize UART module at 9600 bps
     Delay_ms(100);                      // Wait for UART module to stabilize
     DDRB = 0b11111000;                  // PORTB as output, save PB0, PB1 and PB2 which aren't used
     PORTB = 0;                          // Initialize portb

 while (1)
 {
       if (UART1_Data_Ready())           // If data is received,
       {
          uart_rd = UART1_Read();        // read the received data
       }
       action();                         // write outputs
       position();                       // posistion servo
 }
}
