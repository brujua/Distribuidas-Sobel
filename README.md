# Distribuidas-Sobel
A distributed Sobel convolution done in parallel by slicing the image and giving each piece to each remote worker node via RMI.
The main thread monitors the threads that make the remote call, and if there is an error, it gives the pending slice to another node.
When all the processing has been done, it reconstructs the image.

### Example

![Distributed Sobel diagram](https://media.giphy.com/media/1BfQxZVlXFQOTwmqHt/giphy.gif)



